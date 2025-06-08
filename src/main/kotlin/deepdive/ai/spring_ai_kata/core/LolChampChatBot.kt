package deepdive.ai.spring_ai_kata.core

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Component

@Component
class LolChampChatBot(
    private val chatClient: ChatClient,
    private val chatMemory: ChatMemory,
    private val vectorStore: VectorStore,
    private val reranker: CrossEncoderReranker
) {
    private val logger = KotlinLogging.logger {}
    private val topK = 4

    fun ask(sessionId: String, question: String): String {
        val candidates = vectorStore.similaritySearch(question)?.take(topK) ?: emptyList()
        val ragDoc = try {
            reranker.rerank(question, candidates).firstOrNull()
        } catch (ex: Exception) {
            logger.warn(ex) { "Reranking failed, falling back to similarity search" }
            candidates.firstOrNull()
        } ?: return "챗봇이 응답하지 않았습니다. 다시 시도해주세요."

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