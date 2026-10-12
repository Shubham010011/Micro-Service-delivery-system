package com.realtime.delivery.delivery.service;

import com.realtime.delivery.delivery.client.NotificationServiceClient;
import com.realtime.delivery.delivery.client.OrderServiceClient;
import com.realtime.delivery.delivery.dto.ChatMessageResponse;
import com.realtime.delivery.delivery.exception.DeliveryException;
import com.realtime.delivery.delivery.model.ChatMessage;
import com.realtime.delivery.delivery.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final OrderServiceClient orderServiceClient;
    private final NotificationServiceClient notificationServiceClient;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatMessageResponse sendMessage(String senderId, String senderRole, String orderId, String text) {
        String otherPartyId = resolveOtherParty(senderId, orderId);

        ChatMessage message = ChatMessage.builder()
                .orderId(orderId)
                .senderId(senderId)
                .senderRole(senderRole)
                .message(text)
                .build();
        message = chatMessageRepository.save(message);
        ChatMessageResponse response = ChatMessageResponse.from(message);

        // Live push to anyone subscribed to /topic/chat/{orderId} on this service's own WebSocket.
        messagingTemplate.convertAndSend("/topic/chat/" + orderId, response);

        // Guaranteed delivery even if the other party isn't connected to the chat socket right now.
        notificationServiceClient.notifyUser(otherPartyId, "CHAT_MESSAGE", "New message",
                text, Map.of("orderId", orderId, "senderId", senderId));

        return response;
    }

    public List<ChatMessageResponse> history(String requesterId, String orderId) {
        resolveOtherParty(requesterId, orderId); // throws if requester isn't a participant
        return chatMessageRepository.findAllByOrderIdOrderBySentAtAsc(orderId)
                .stream().map(ChatMessageResponse::from).toList();
    }

    /** Confirms the requester is a participant in this order's delivery and returns the OTHER party's id. */
    private String resolveOtherParty(String requesterId, String orderId) {
        Map<String, Object> order = orderServiceClient.getOrder(orderId);
        String customerId = (String) order.get("customerId");
        String partnerId = (String) order.get("assignedDeliveryPartnerId");

        if (requesterId.equals(customerId)) {
            if (partnerId == null) throw DeliveryException.conflict("No delivery partner assigned to this order yet");
            return partnerId;
        }
        if (requesterId.equals(partnerId)) {
            return customerId;
        }
        throw DeliveryException.forbidden("You are not a participant in this order's delivery");
    }
}
