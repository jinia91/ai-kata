package deepdive.ai.spring_ai_kata.inbound

import deepdive.ai.spring_ai_kata.core.EmbeddingService
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/embedding")
class EmbeddingDbController(
    private val embeddingDbService: EmbeddingService
) {

    @PostMapping("/init")
    fun init() {
        embeddingDbService.init()
    }
}