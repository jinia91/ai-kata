package deepdive.ai.spring_ai_kata.core

import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.chat.messages.AssistantMessage
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Component

@Component
class LolChampChatBot(
    private val chatClient: ChatClient,
    private val chatMemory: ChatMemory,
    private val vectorStore: VectorStore,
) {
    fun ask(sessionId: String, question: String): String {
        // 1. 쿼리 변환 (Query Rewriting)
        val rewrittenQuestion = rewriteQuery(question)
        println("Rewritten question: $rewrittenQuestion")

        val request = SearchRequest.builder()
            .query(rewrittenQuestion)
            .topK(10)
            .build()
        val ragDoc = vectorStore.similaritySearch(request)

        // 2. 메타 데이터의 챔피언이름으로 그룹핑
        val map = ragDoc?.groupBy { it.metadata["name"] as String }

        // 3. 가장 많이 선택된 챔피언의 문서
        val championEntry = map?.maxByOrNull { it.value.size }

        val response = chatClient
            .prompt()
            .advisors(
                SimpleLoggerAdvisor(),
                MessageChatMemoryAdvisor.builder(chatMemory)
                    .conversationId(sessionId)
                    .build()
            )
            .messages(
                AssistantMessage(
                    "벡터 DB에서 조회해온 질문과 유사한 챔피언 정보 :" +
                    "이름 : ${championEntry?.key ?: "알 수 없음"}\n" +
                    "문서 수 : ${championEntry?.value?.size ?: 0}\n" +
                    "문서 내용 : ${championEntry?.value?.joinToString("\n") { it.text ?: "내용 없음" }}"
                ),
                UserMessage(question), // 사용자의 원본 질문을 전달
            ).call()
            .content()

        return response ?: "챗봇이 응답하지 않았습니다. 다시 시도해주세요."
    }

    private fun rewriteQuery(question: String): String {
        val userMessage = UserMessage(question)

        val rewritten = chatClient.prompt()
            .messages(AssistantMessage(" 다음 사용자 질문을 벡터 데이터베이스 검색에 더 적합하도록 명확하고 상세한 검색 쿼리로 변환해 주세요.\n" +
                    "            챔피언 이름, 특징, 역할 등과 같은 구체적인 키워드를 포함하여 검색 결과의 정확도를 높이는 것이 목표입니다.\n" +
                    "            변환된 쿼리만 답변으로 제공하고, 다른 설명은 덧붙이지 마세요."), userMessage)
            .call()
            .content()

        return rewritten ?: question
    }
}