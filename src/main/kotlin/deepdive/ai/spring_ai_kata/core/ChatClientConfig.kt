package deepdive.ai.spring_ai_kata.core

import org.springframework.ai.chat.client.ChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ChatClientConfig(
    private val chatBuilder: ChatClient.Builder,
) {
    @Bean
    fun chatClient(): ChatClient {
        return chatBuilder
            .defaultSystem("너는 리그 오브 레전드, LOL, 리오레의 챔피언 설명 봇이다. 최대한 사실을 기반으로 대답한다")
            .build()
    }
}