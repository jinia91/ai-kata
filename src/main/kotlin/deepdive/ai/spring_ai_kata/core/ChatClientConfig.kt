package deepdive.ai.spring_ai_kata.core

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ChatClientConfig(
    private val chatBuilder: ChatClient.Builder,
) {
    @Bean
    fun chatClient(): ChatClient {
        return chatBuilder
            .defaultSystem("너는 평범한 챗봇이다")
            .build()
    }
}