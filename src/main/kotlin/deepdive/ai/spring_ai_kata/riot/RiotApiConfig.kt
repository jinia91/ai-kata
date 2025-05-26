package deepdive.ai.spring_ai_kata.riot

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class RiotApiConfig {
    @Bean
    fun riotWebClient(): WebClient {
        return WebClient.builder()
            .baseUrl("https://asia.api.riotgames.com")
            .defaultHeader("X-Riot-Token", "{API_KEY}")
            .build()
    }
}