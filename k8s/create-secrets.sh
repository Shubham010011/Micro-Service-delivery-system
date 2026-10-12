#!/usr/bin/env bash
# Creates (or rotates) the swiftdrop-secrets Secret with freshly generated strong values.
# Usage:  GOOGLE_CLIENT_ID=xxx ADMIN_EMAIL=you@example.com ADMIN_PASSWORD='...' ./k8s/create-secrets.sh
# Re-running REPLACES the secret: all users are logged out (new JWT_SECRET) and DB_PASSWORD must match Postgres,
# so on an existing cluster pass DB_PASSWORD=<current password> to keep it.
set -euo pipefail
kubectl get namespace swiftdrop >/dev/null 2>&1 || kubectl apply -f "$(dirname "$0")/00-namespace.yaml"

args=(--from-literal=JWT_SECRET="${JWT_SECRET:-$(openssl rand -base64 48)}"
      --from-literal=DB_PASSWORD="${DB_PASSWORD:-$(openssl rand -hex 24)}"
      --from-literal=GOOGLE_CLIENT_ID="${GOOGLE_CLIENT_ID:-REPLACE_WITH_YOUR_GOOGLE_OAUTH_CLIENT_ID}")
for v in ADMIN_EMAIL ADMIN_PASSWORD; do
  [ -n "${!v:-}" ] && args+=(--from-literal="$v=${!v}")
done

kubectl -n swiftdrop create secret generic swiftdrop-secrets "${args[@]}" --dry-run=client -o yaml | kubectl apply -f -
echo "swiftdrop-secrets applied."
