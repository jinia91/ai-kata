//package deepdive.ai.spring_ai_kata
//
//import org.springframework.ai.embedding.EmbeddingModel
//import org.springframework.context.annotation.Bean
//import org.springframework.context.annotation.Configuration
//import org.springframework.web.reactive.function.client.WebClient
//
//
//@Configuration
//class EmbeddingConfig {
//    @Bean
//    fun embeddingModel(): EmbeddingModel {
//        val webClient = WebClient.builder()
//            .baseUrl("http://localhost:7860")
//            .build()
//
//        return EmbeddingModel(webClient, "/embed") {
//            mapOf("sentences" to listOf(it.text()))
//        }
//    }
//
//
//}