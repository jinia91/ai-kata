package deepdive.ai.spring_ai_kata

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.stereotype.Component

@Component
class LolChampChatBot(
    private val chatClient: ChatClient,
    private val chatMemory: ChatMemory
) {
    fun ask(sessionId: String, question: String): String {
        val response = chatClient
            .prompt()
            .advisors (
                SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory)
                    .conversationId(sessionId)
                    .build()
            )
            .messages(
                UserMessage(question),
            ).call()
            .content()

        return response ?: "챗봇이 응답하지 않았습니다. 다시 시도해주세요."
    }
}