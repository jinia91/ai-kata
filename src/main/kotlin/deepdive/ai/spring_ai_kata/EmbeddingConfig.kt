package deepdive.ai.spring_ai_kata

import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.ollama.OllamaEmbeddingModel
import org.springframework.ai.ollama.api.OllamaApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class EmbeddingConfig {
    @Bean
    fun ollamaApi(): OllamaApi {
        return OllamaApi.builder().build()
    }


    @Bean
    fun embeddingModel(): EmbeddingModel {
        return OllamaEmbeddingModel
            .builder()
            .ollamaApi(ollamaApi())
            .build()
    }
}