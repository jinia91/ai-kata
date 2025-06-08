package deepdive.ai.spring_ai_kata.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.ai.document.Document
import org.springframework.web.reactive.function.client.WebClient

class CrossEncoderRerankerTests {
    @Test
    fun fallbackWhenServiceUnavailable() {
        val reranker = CrossEncoderReranker(WebClient.builder().baseUrl("http://localhost:0"))
        val docs = listOf(Document("id", "text", emptyMap()))
        val result = reranker.rerank("query", docs)
        assertEquals(docs, result)
    }
}
