package deepdive.ai.spring_ai_kata.core

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.document.Document
import org.springframework.ai.rag.Query
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class LolChampChatBot(
    private val chatClient: ChatClient,
    private val chatMemory: ChatMemory,
    private val vectorStore: VectorStore,
    private val chatBuilder: ChatClient.Builder,
) {
    fun ask(sessionId: String, question: String): String {
        val rewriter = RewriteQueryTransformer.builder()
            .chatClientBuilder(chatBuilder.defaultOptions(ChatOptions.builder().temperature(0.0).build()).build().mutate())
            .build()
        val rewritedQuery = rewriter.transform(Query(question))
            .also { logger.info { "Rewrited query: ${it.text}" } }

        val dbRetriever = VectorStoreDocumentRetriever.builder()
            .similarityThreshold(0.3)
            .topK(10)
            .vectorStore(vectorStore)
            .build()

        val documents = dbRetriever.retrieve(rewritedQuery)
            .also { logger.info { "Retrieved documents: ${it.size}" } }

        if (documents.isEmpty()) {
            return "관련 문서를 찾을 수 없습니다."
        }

        // RRF-based group selection
        val k = 60
        val groupScores = mutableMapOf<String, Double>()
        documents.withIndex().forEach { (rank, doc) ->
            val groupName = doc.metadata["name"] as? String
            if (groupName != null) {
                groupScores[groupName] = (groupScores.getOrDefault(groupName, 0.0)) + 1.0 / (k + rank)
            }
        }

        val mostRelevantGroupName = groupScores.maxByOrNull { it.value }?.key

        val filteredDocuments = if (mostRelevantGroupName != null) {
            documents.filter { (it.metadata["name"] as String?) == mostRelevantGroupName }
        } else {
            documents
        }.also { logger.info { "Filtered documents count for group '$mostRelevantGroupName': ${it.size}" } }


        val documentsContext = filteredDocuments.joinToString("\n\n") { it.text!! }

        val answer: String? = chatClient.prompt()
            .advisors(
                SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory)
                    .conversationId(sessionId)
                    .build()
            )
            .messages(AssistantMessage("""
                다음은 사용자의 질문과 관련하여 가장 관련성이 높은 그룹에서 검색된 문서 목록입니다:
                $documentsContext
            """.trimIndent()))
            .user(question)
            .call()
            .content()

        return answer ?: "챗봇이 응답하지 않았습니다. 다시 시도해주세요."
    }
}