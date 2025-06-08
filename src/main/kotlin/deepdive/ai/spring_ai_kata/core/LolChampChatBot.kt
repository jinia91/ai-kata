package deepdive.ai.spring_ai_kata.core

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Component

@Component
class LolChampChatBot(
    private val chatClient: ChatClient,
    private val chatMemory: ChatMemory,
    private val vectorStore: VectorStore
) {
    internal fun summarizeDocuments(documents: List<Document>): String {
        return documents.joinToString(separator = "\n") { doc ->
            val championName = doc.metadata["championName"] ?: doc.metadata["name"]
            "챔피언 이름: $championName, 내용: ${doc.text}"
        }
    }

    fun ask(sessionId: String, question: String): String {
        val request = SearchRequest.builder()
            .query(question)
            .topK(3)
            .build()
        val ragDocs = vectorStore.similaritySearch(request)!!.takeIf { it.isNotEmpty() }
            ?: return "챗봇이 응답하지 않았습니다. 다시 시도해주세요."
        val summary = summarizeDocuments(ragDocs)

        val response = chatClient
            .prompt()
            .advisors (
                SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory)
                    .conversationId(sessionId)
                    .build()
            )
            .messages(
                AssistantMessage("벡터 DB에서 조회해온 질문과 유사한 챔피언 정보:\n$summary"),
                UserMessage(question),
            ).call()
            .content()

        return response ?: "챗봇이 응답하지 않았습니다. 다시 시도해주세요."
    }
}