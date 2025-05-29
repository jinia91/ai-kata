package deepdive.ai.spring_ai_kata

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.handler.annotation.SendTo
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.stereotype.Controller
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import java.util.concurrent.ConcurrentHashMap
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig : WebSocketMessageBrokerConfigurer {
    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/chat").setAllowedOriginPatterns("*")
    }

    override fun configureMessageBroker(registry: MessageBrokerRegistry) {
        registry.setApplicationDestinationPrefixes("/app")
        registry.enableSimpleBroker("/topic")
    }
}


data class ChatMessage(
    val sessionId: String,
    val message: String
)

@Controller
class ChatController(
    private val chatClient: ChatClient
) {
    private val sessionContext: MutableMap<String, MutableList<String>> = ConcurrentHashMap()
    private val logger = KotlinLogging.logger {}

    @MessageMapping("/ask")
    @SendTo("/topic/reply")
    fun handleMessage(
        @Payload chatMessage: ChatMessage,
    ): String {
        val sessionId = chatMessage.sessionId
        val message = chatMessage.message
        val history = sessionContext.computeIfAbsent(sessionId) { mutableListOf() }
        history.add("user: $message")
        logger.info { "Received message from session $sessionId: $message" }
        logger.info { "Current history for session $sessionId: $history" }

        val response = chatClient
            .prompt()
            .system(systemPrompt)
            .messages(
                history.map { message ->
                    val (role, content) = message.split(": ", limit = 2)
                    when (role) {
                        "user" -> {
                            UserMessage(content)
                        }
                        "assistant" -> {
                            AssistantMessage(content)
                        }
                        else -> {
                            throw IllegalArgumentException("Unknown role: $role")
                        }
                    }
                }
            ).call()
            .content()
        history.add("assistant: $response")
        return response ?: "응답을 생성할 수 없습니다."
    }
}