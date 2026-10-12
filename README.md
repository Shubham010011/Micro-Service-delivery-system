# SwiftDrop — Real-Time Delivery Tracking System

A microservices-based real-time delivery tracking platform (Spring Boot + Spring Cloud), inspired by
Zomato/Swiggy-style delivery flows: customers place orders, delivery partners accept/reject and update
status live, both sides can chat with each other without exposing phone numbers,
and admins can view and manually assign orders.

## Architecture

```
                                   ┌─────────────────┐
                                   │   Frontend (SPA) │  customer.html / delivery.html / admin.html
                                   └────────┬─────────┘
                                            │ HTTPS + WSS (single origin)
                                   ┌────────▼─────────┐
                                   │   API Gateway     │  JWT validation, CORS, role-based routing
                                   │ (Spring Cloud GW) │  hides /admin and /staff from customers
                                   └────────┬─────────┘
                    ┌───────────┬───────────┼───────────┬────────────┐
              ┌─────▼────┐ ┌────▼─────┐ ┌───▼──────┐ ┌──▼───────────┐
              │  auth-   │ │  order-  │ │ delivery-│ │ notification-│
              │ service  │ │ service  │ │ service  │ │   service     │
              └────┬─────┘ └────┬─────┘ └────┬─────┘ └──────┬───────┘
                   │            │            │              │
                   └────────────┴─────┬──────┴──────────────┘
                                       │  registers with / discovered via
                                 ┌─────▼─────┐
                                 │  Eureka   │
                                 └───────────┘
```

- **api-gateway** — the only component reachable from outside the cluster. Validates every JWT,
  strips any client-supplied `X-User-*` headers (so they can never be spoofed), re-derives them from
  the verified token, and rejects `/admin/**` and `/staff/**` requests before they ever reach a
  downstream service unless the caller's role matches. This is what keeps the admin panel and the
  delivery-partner ("staff") endpoints invisible/unreachable to ordinary customers.
- **auth-service** — registration & login for customers and delivery partners. Passwords are hashed
  with BCrypt (strength 12) before they touch the database — plaintext is never stored or logged.
  Issues JWTs (HS256) and verifies Google ID tokens for "Sign in with Google".
- **order-service** — order placement/cancellation (customer), all-orders view + manual assignment
  (admin), and the source of truth for order status.
- **delivery-service** — covers both "Delivery" and "Driver" from the spec (they're the same actor:
  the delivery partner). Availability, new-order broadcast, accept/reject, status updates
  (picked up → in transit → delivered), plus real-time chat between customer and delivery partner.
  Calling is intentionally removed in this version.
- **notification-service** — persists notifications and pushes them live over STOMP/WebSocket
  (new order offers to drivers, status updates to customers, and chat alerts).
- **eureka-server** — service discovery; every service registers here and the gateway/services call
  each other by logical name (`http://order-service`, etc.) instead of hardcoded hosts.

Internal endpoints under `/internal/**` are **never** registered as gateway routes, so they are unreachable
from outside the cluster by construction — not just by a role check.

## Tech stack

Java 17 · Spring Boot 3.2 · Spring Cloud 2023 (Gateway, Eureka, LoadBalancer) · Spring Security + JWT
(jjwt) · Spring Data JPA + PostgreSQL · Spring WebSocket/STOMP · BCrypt · Google ID token verification ·
Docker · Kubernetes · Prometheus + Grafana · ELK (Elasticsearch, Logstash, Kibana, Filebeat).

## Repository layout

```
common/                  shared JWT util, role/header constants, DTOs (used by every service)
eureka-server/           service discovery
api-gateway/             single entry point, JWT + CORS + routing
auth-service/            register/login/Google sign-in, BCrypt hashing
order-service/           orders: place/cancel/view, admin assignment
delivery-service/        driver profile, accept/reject, status, real-time chat
notification-service/    WebSocket push + notification history
frontend/                plain HTML/CSS/JS SPA (customer, delivery, admin, login pages)
k8s/                     Kubernetes manifests (numbered, apply in order)
monitoring/              Prometheus + Grafana provisioning
elk/                     Logstash pipeline + Filebeat config
docker-compose.yml        full local stack
init-scripts/            Postgres multi-database init script
```

## Running locally with Docker Compose

**Prerequisites:** Docker with Compose v2 (the images are built inside Docker, which downloads dependencies from Maven Central).

```bash
cp .env.example .env        # then edit: set JWT_SECRET (openssl rand -base64 48), DB_PASSWORD, ADMIN_EMAIL/ADMIN_PASSWORD
docker compose up --build
```

This brings up: Postgres (4 databases via the init script), Eureka (:8761, localhost only), api-gateway (:8080),
all 4 services, the frontend (nginx on :3000), Prometheus (:9090), Grafana (:3001), and the ELK stack.
The four business services are **not** published to the host — the gateway is the only way in. Compose
publishes Postgres and Eureka on 127.0.0.1 only.

Open **http://localhost:3000**. The frontend talks to the gateway at `http://localhost:8080`
(change `window.API_BASE` in the HTML files if you move the gateway).

### Running a service without Docker / Postgres

Each service defaults to a local PostgreSQL (`localhost:5432`, db `authdb`/`orderdb`/`deliverydb`/`notificationdb`,
user/password `swiftdrop`). For a quick in-memory run with no database, start it with the `h2` profile:

```bash
export JWT_SECRET=$(openssl rand -base64 48)   # required, identical for the gateway and all services
mvn -pl auth-service spring-boot:run -Dspring-boot.run.profiles=h2
```

With Postgres instead, also `export DB_PASSWORD=...` (services refuse to start without `JWT_SECRET`, and reject
placeholder/short values — there is deliberately no built-in default secret).

Start order: `eureka-server` → the four services → `api-gateway`.

Register a customer and a delivery partner account, then:
1. Log in as the delivery partner (`delivery.html`) and click **Available**.
2. Log in as the customer (`customer.html`, a different browser/incognito window) and place an order.
3. Watch the delivery partner's tab receive the live offer, accept it, and update status —
   the customer's tab updates in real time via the notification WebSocket.
4. Open an assigned order to use the in-order chat; calling is intentionally not part of this version.
5. Log in as an admin (self-registration only allows CUSTOMER/DELIVERY_PARTNER — see below for
   creating an admin) to see all orders and manually assign a partner.

### Creating the first admin account

Admins can't self-register. Set `ADMIN_EMAIL` and `ADMIN_PASSWORD` (in `.env`, or as environment variables of
auth-service); the account is created on startup if it doesn't exist. Remove the variables after first login.

### How dispatch works

When an order is placed it is offered to every AVAILABLE partner at once (or first to the requested "same partner",
if they're available); the first to accept wins. Each offer expires after `DISPATCH_OFFER_TTL_SECONDS` (default 90).
A scheduler retries every order that still has no partner every few seconds, so an order placed while nobody is
online is offered as soon as a partner goes AVAILABLE. Partners who rejected an order are never asked again. If no
one accepts within `DISPATCH_GIVE_UP_MINUTES` (default 30) the order is cancelled and the customer is notified.


### Database schema

Schemas are owned by Flyway (`src/main/resources/db/migration`, applied automatically on startup; databases created
by an older `ddl-auto=update` run are adopted). Add a new `V2__...sql` for every future entity change. Set
`DDL_AUTO=validate` to have Hibernate cross-check the entities against the schema.

### Known limitations

- Offers to partners are pushed via the notification WebSocket; there are no mobile push notifications.

## Kubernetes deployment

Manifests live in `k8s/`, numbered so `kubectl apply -f k8s/` (or apply one-by-one) works in the
right order. Before applying:

1. **Build and push images** for each service to a registry your cluster can pull from, matching the
   image names referenced in the manifests (`swiftdrop/auth-service:1.0.0`, etc.):
   ```bash
   mvn clean package -DskipTests   # from the repo root, builds every module's jar
   for svc in eureka-server api-gateway auth-service order-service delivery-service notification-service; do
     docker build -t your-registry/swiftdrop/$svc:1.0.0 $svc
     docker push your-registry/swiftdrop/$svc:1.0.0
   done
   docker build -t your-registry/swiftdrop/frontend:1.0.0 frontend
   docker push your-registry/swiftdrop/frontend:1.0.0
   ```
   Update the `image:` fields in `k8s/*.yaml` to match your registry.
2. **Create the secrets** — `k8s/02-secrets.yaml.example` is only a template (no real secrets are committed). Run
   `GOOGLE_CLIENT_ID=... ADMIN_EMAIL=... ADMIN_PASSWORD=... ./k8s/create-secrets.sh` to generate a strong
   `JWT_SECRET` and `DB_PASSWORD` and apply the Secret (or manage it via your cluster's secret manager).
3. **Edit `k8s/09-api-gateway.yaml`** — replace `api.swiftdrop.example.com` / `swiftdrop.example.com`
   with your real domains, and make sure an ingress-nginx controller is installed.
4. Apply everything:
   ```bash
   kubectl apply -f k8s/00-namespace.yaml
   kubectl apply -f k8s/
   ```
5. Watch rollout: `kubectl -n swiftdrop get pods -w`

Only `api-gateway` and `swiftdrop-frontend` are reachable via the Ingress — every other service
(including Postgres, Eureka, and all four microservices) is a `ClusterIP` Service with no external
route, which is the other half of "customers can't see `/admin` or `/staff`": even if someone knew
the exact internal URL, there's no network path to it from outside the cluster.

## Monitoring & logs

- **Prometheus** scrapes `/actuator/prometheus` on every service (via Kubernetes pod annotations in
  the cluster, or static targets in `monitoring/prometheus/prometheus.yml` for Compose).
- **Grafana** (`GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` from `.env`, default `admin` / `admin` — change this) comes with a starter dashboard
  ("SwiftDrop - Service Overview": request rate, JVM heap, p95 latency, 5xx rate per service).
- **ELK**: every service logs to stdout in a Logstash-friendly pattern; Filebeat tails container logs
  (Docker autodiscover in Compose, Kubernetes autodiscover in the cluster) and ships them to Logstash,
  which indexes into Elasticsearch as `swiftdrop-logs-YYYY.MM.dd`. Explore them in Kibana at
  `http://localhost:5601` (Compose) — create an index pattern `swiftdrop-logs-*` on first visit.

## Security notes / design decisions

- **Password hashing**: BCrypt strength 12, in `auth-service`'s `SecurityBeansConfig` +
  `AuthService.register`. Plaintext passwords are discarded immediately after hashing.
- **JWT**: HS256, shared secret across all services (`JWT_SECRET`), 24h validity by default
  (`jwt.access-token-validity-ms`). The gateway validates the token on every request; each service
  *also* re-validates it independently if called directly (defense in depth for local/dev testing).
- **Hiding admin/staff endpoints**: enforced at the gateway (role check before routing) — not just
  security-through-obscurity. `/admin/**` and `/staff/**` are also never referenced by
  `customer.html`/its JS, so a customer has no way to discover them exist.
- **Customer ↔ delivery-partner communication**: chat uses WebSocket/STOMP. Phone calling is intentionally removed; phone numbers are not exposed to the other party.
- **Driver vs. Delivery**: the spec lists them as separate services, but a "driver" and a "delivery
  partner" are the same account/actor in this domain, so they're implemented together in
  `delivery-service` rather than split into two services that would just call each other constantly.

## What's a stub / what you'd still do before real production use

- The external product catalogue is a demo/test API and should be replaced with a persistent product/inventory service before production.
- No refresh-token flow (single 24h access token) — add refresh tokens for production.
- No rate limiting on the gateway — Spring Cloud Gateway supports a Redis-backed rate limiter if needed.
- Postgres runs as a single Deployment with an `emptyDir`/PVC, not a managed HA database — use a
  managed Postgres (RDS/Cloud SQL) or an operator (Zalando/CloudNativePG) in real production.
- Elasticsearch/Kibana run single-node with no auth (`xpack.security.enabled: false`) — fine for a
  local/demo ELK stack, not for production.
