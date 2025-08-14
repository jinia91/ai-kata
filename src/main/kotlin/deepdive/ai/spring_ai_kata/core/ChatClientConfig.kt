package deepdive.ai.spring_ai_kata.core

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ChatClientConfig(
    private val chatBuilder: ChatClient.Builder,
) {
    @Bean
    fun chatClient(): ChatClient {
        return chatBuilder
            .defaultOptions(
                ChatOptions.builder()
                    .temperature(0.0)
                    .build()
            )
            .build()
    }
}