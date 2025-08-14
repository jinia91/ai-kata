package deepdive.ai.spring_ai_kata.core

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
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
            .messages(
                SystemMessage(
                    """
너는 리그 오브 레전드, LOL, 롤의 챔피언 설명 봇이다. 제공되는 문맥을 참고하여 최대한 사실을 기반으로 대답한다. 답을 모르거나 문맥이 부족한경우 모른다고만 말한다. 최대 세문장으로 간결하게 답변해라
                    <CONTEXT>
                    $documentsContext
                    </CONTEXT>
                    """.trimIndent()
                ),
                UserMessage(question)
            )
            .call()
            .content()

        return answer ?: "챗봇이 응답하지 않았습니다. 다시 시도해주세요."
    }
}