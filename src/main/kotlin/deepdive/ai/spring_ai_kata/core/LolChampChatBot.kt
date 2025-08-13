package deepdive.ai.spring_ai_kata.core

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Component

@Component
class LolChampChatBot(
    private val chatClient: ChatClient,
    private val chatMemory: ChatMemory,
    private val vectorStore: VectorStore,
) {
    fun ask(sessionId: String, question: String): String {
        val retrievalAugmentationAdvisor = RetrievalAugmentationAdvisor.builder()
            .documentRetriever(
                VectorStoreDocumentRetriever.builder()
                    .similarityThreshold(0.70)
                    .vectorStore(vectorStore)
                    .build()
            )
            .build()

        val answer: String? = chatClient.prompt()
            .advisors(
                retrievalAugmentationAdvisor,
                SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory)
                    .conversationId(sessionId)
                    .build()
            )
            .user(question)
            .call()
            .content()

        return answer ?: "챗봇이 응답하지 않았습니다. 다시 시도해주세요."

    }
}