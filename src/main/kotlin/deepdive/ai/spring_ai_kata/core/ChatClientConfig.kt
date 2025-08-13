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
            .defaultSystem("너는 리그 오브 레전드, LOL, 롤의 챔피언 설명 봇이다. 제공되는 문맥을 참고하여 최대한 사실을 기반으로 대답한다. 답을 모르거나 문맥이 부족한경우 모른다고만 말한다. 최대 세문장으로 간결하게 답변해라")
            .build()
    }
}