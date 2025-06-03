package deepdive.ai.spring_ai_kata

import io.github.oshai.kotlinlogging.KotlinLogging
import java.security.Principal
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.stereotype.Controller
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.server.support.DefaultHandshakeHandler


private val logger = KotlinLogging.logger {}

class ChatHandshakeHandler : DefaultHandshakeHandler() {
    override fun determineUser(
        request: ServerHttpRequest,
        wsHandler: WebSocketHandler,
        attributes: Map<String?, Any?>
    ): Principal {
        val query = request.uri.query ?: return Principal { "guest" }
        val userName = query.split("&").firstNotNullOfOrNull {
            it.split("=").takeIf { parts -> parts.size == 2 && parts[0] == "sessionId" }?.get(1)
        }
        return Principal { userName ?: "guest" }
    }
}

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/chat").setAllowedOriginPatterns("*")
            .setHandshakeHandler(ChatHandshakeHandler())
            .withSockJS()
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.setApplicationDestinationPrefixes("/app")
        registry.setUserDestinationPrefix("/user")
        registry.enableSimpleBroker("/user")
    }
}

data class ChatMessage(
    val sessionId: String,
    val message: String,
)

@Controller
class ChatController(
    private val lolChampChatBot: LolChampChatBot,
    private val messagingTemplate: SimpMessagingTemplate,
) {
    @MessageMapping("/ask")
    fun handleMessage(
        @Payload chatMessage: ChatMessage,
        principal: Principal
    ) {
        logger.info { "사용자: ${principal.name} 질의 수신 : $chatMessage" }
        logger.info { "질의 수신 : $chatMessage" }
        val reply = lolChampChatBot.ask(
            sessionId = chatMessage.sessionId,
            question = chatMessage.message
        )
        logger.info { "챗봇 응답 : $reply" }

        messagingTemplate.convertAndSendToUser(
            principal.name,
            "/reply",
            reply
        )
    }
}