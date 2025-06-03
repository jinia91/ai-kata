package deepdive.ai.spring_ai_kata.inbound

import deepdive.ai.spring_ai_kata.core.LolChampChatBot
import java.security.Principal
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Controller

@Controller
class AiChatHandler(
    private val lolChampChatBot: LolChampChatBot,
    private val messagingTemplate: SimpMessagingTemplate,
) {
    data class ChatMessage(
        val sessionId: String,
        val message: String,
    )

    @MessageMapping("/ask")
    fun handleMessage(
        @Payload chatMessage: ChatMessage,
        principal: Principal
    ) {
        val reply = lolChampChatBot.ask(
            sessionId = chatMessage.sessionId,
            question = chatMessage.message
        )
        messagingTemplate.convertAndSendToUser(
            principal.name,
            "/reply",
            reply
        )
    }
}