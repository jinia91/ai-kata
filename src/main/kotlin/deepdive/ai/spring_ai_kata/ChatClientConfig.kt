package deepdive.ai.spring_ai_kata

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ChatClientConfig(
    private val chatBuilder: ChatClient.Builder,
    private val chatMemory: ChatMemory,
) {
    @Bean
    fun chatClient(): ChatClient {
        return chatBuilder
            .defaultSystem("너는 평범한 챗봇이다")
            .build()
    }
}
