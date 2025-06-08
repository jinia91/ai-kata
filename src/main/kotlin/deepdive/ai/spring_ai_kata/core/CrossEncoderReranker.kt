package deepdive.ai.spring_ai_kata.core

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.ai.document.Document
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class CrossEncoderReranker(
    builder: WebClient.Builder,
) {
    private val logger = KotlinLogging.logger {}

    private val webClient = builder
        .baseUrl("http://localhost:8002")
        .build()

    data class RerankRequest(val query: String, val documents: List<String>)
    data class RerankResponse(val scores: List<Double>)

    fun rerank(query: String, documents: List<Document>): List<Document> {
        if (documents.isEmpty()) {
            return documents
        }
        val req = RerankRequest(query, documents.map { it.text })
        val scores = try {
            webClient.post()
                .uri("/rerank")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(RerankResponse::class.java)
                .block()?.scores
        } catch (ex: Exception) {
            logger.warn(ex) { "Cross-encoder rerank request failed" }
            null
        }
        return if (scores != null && scores.size == documents.size) {
            documents.zip(scores)
                .sortedByDescending { it.second }
                .map { it.first }
        } else {
            documents
        }
    }
}

