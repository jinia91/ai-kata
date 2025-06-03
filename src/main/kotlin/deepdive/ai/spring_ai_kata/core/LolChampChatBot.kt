package deepdive.ai.spring_ai_kata.core

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Component

@Component
class LolChampChatBot(
    private val chatClient: ChatClient,
    private val chatMemory: ChatMemory,
    private val vectorStore: VectorStore
) {
    fun ask(sessionId: String, question: String): String {
        val ragDoc = vectorStore.similaritySearch(question)!!.firstOrNull()
            ?: return "챗봇이 응답하지 않았습니다. 다시 시도해주세요."

        val response = chatClient
            .prompt()
            .advisors (
                SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory)
                    .conversationId(sessionId)
                    .build()
            )
            .messages(
                AssistantMessage("벡터 DB에서 조회해온 질문과 유사한 챔피언 정보 :" +
                        "챔피언 이름: ${ragDoc.metadata["championName"]}, " +
                        "챔피언 설명: ${ragDoc.text}"),
                UserMessage(question),
            ).call()
            .content()

        return response ?: "챗봇이 응답하지 않았습니다. 다시 시도해주세요."
    }
}