package deepdive.ai.spring_ai_kata.inbound

import java.security.Principal
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.ServerHttpRequest
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer
import org.springframework.web.socket.server.support.DefaultHandshakeHandler

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