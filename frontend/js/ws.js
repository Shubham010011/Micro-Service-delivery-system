// Live notifications - connects once per session, any authenticated role can use it.
function connectNotifications(onNotification) {
  const token = Auth.getToken();
  if (!token) return null;
  const socket = new SockJS(`${API_BASE}/ws/notifications?token=${encodeURIComponent(token)}`);
  const client = Stomp.over(socket);
  client.debug = null; // silence verbose STOMP frame logging
  client.connect({}, () => {
    client.subscribe('/user/queue/notifications', (frame) => {
      const body = JSON.parse(frame.body);
      onNotification(body);
    });
  }, () => { /* connection dropped - the REST notification history still works as a fallback */ });
  return client;
}

// Live order chat between a customer and their assigned delivery partner. Real phone numbers are
// never part of this payload - only text messages, so there is nothing to mask here.
function connectOrderChat(orderId, onMessage) {
  const token = Auth.getToken();
  if (!token) return null;
  const socket = new SockJS(`${API_BASE}/ws/chat?token=${encodeURIComponent(token)}`);
  const client = Stomp.over(socket);
  client.debug = null;
  client.connect({}, () => {
    client.subscribe(`/topic/chat/${orderId}`, (frame) => {
      onMessage(JSON.parse(frame.body));
    });
  });
  return client;
}
