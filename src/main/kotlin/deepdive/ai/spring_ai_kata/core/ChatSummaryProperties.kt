package deepdive.ai.spring_ai_kata.core

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("chat.summary")
class ChatSummaryProperties {
    var trigger: Int = 10
    var length: Int = 3
}
