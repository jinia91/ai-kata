package deepdive.ai.spring_ai_kata

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan

@SpringBootApplication
@ConfigurationPropertiesScan
class SpringAiKataApplication

fun main(args: Array<String>) {
    runApplication<SpringAiKataApplication>(*args)
}
