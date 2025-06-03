package deepdive.ai.spring_ai_kata.core

import org.springframework.ai.document.Document
import org.springframework.ai.embedding.Embedding
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.ai.embedding.EmbeddingResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class EmbeddingConfig {
    data class LocalRequest(
        val sentences: List<String>
    )

    @Bean
    fun embeddingModel(): EmbeddingModel {
        val strategies = ExchangeStrategies.builder()
            .codecs { it.defaultCodecs().maxInMemorySize(10 * 1024 * 1024) }
            .build()

        val webClient = WebClient.builder()
            .baseUrl("http://localhost:8001")
            .exchangeStrategies(strategies)
            .build()


        return object : EmbeddingModel {
            override fun call(request: EmbeddingRequest): EmbeddingResponse {
                val response = webClient.post()
                    .uri("/embed")
                    .bodyValue(LocalRequest(request.instructions))
                    .retrieve()
                    .bodyToMono(Map::class.java)
                    .log()
                    .block()

                @Suppress("UNCHECKED_CAST")
                val vectors = (response?.get("embeddings") as List<List<Double>>)
                    .map { it.map { it.toFloat() }.toFloatArray() }

                val embeddings = vectors.mapIndexed { index, vector ->
                    Embedding(
                        vector,
                        index
                    )
                }

                return EmbeddingResponse(embeddings)
            }

            override fun embed(document: Document): FloatArray {
                val response = webClient.post()
                    .uri("/embed")
                    .bodyValue(mapOf("sentences" to listOf(document.text)))
                    .retrieve()
                    .bodyToMono(Map::class.java)
                    .block()

                @Suppress("UNCHECKED_CAST")
                val embedding = (response?.get("embeddings") as List<List<Double>>)[0]
                return embedding.map { it.toFloat() }.toFloatArray()
            }
        }
    }
}