package deepdive.ai.spring_ai_kata

import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ChatClientConfig(
    private val chatBuilder: ChatClient.Builder
) {
    @Bean
    fun chatClient(): ChatClient {
        return chatBuilder
            .defaultSystem("너는 지하철 노선 안내 봇이야. 사용자가 지하철 노선에 대한 질문을 하면, 정확하고 친절하게 답변해줘. 한글로 답변해줘.")
            .build()
    }
}
