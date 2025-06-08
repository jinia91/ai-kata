package deepdive.ai.spring_ai_kata.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.memory.ChatMemory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore

class LolChampChatBotTest {

    @Test
    fun `summarizeDocuments merges champion texts`() {
        val chatClient = mock(ChatClient::class.java)
        val chatMemory = mock(ChatMemory::class.java)
        val vectorStore = mock(VectorStore::class.java)

        val bot = LolChampChatBot(chatClient, chatMemory, vectorStore)
        val docs = listOf(
            Document("id1", "text1", mapOf("championName" to "A")),
            Document("id2", "text2", mapOf("championName" to "B"))
        )
        val summary = bot.summarizeDocuments(docs)
        assertTrue(summary.contains("A"))
        assertTrue(summary.contains("text1"))
        assertTrue(summary.contains("B"))
        assertTrue(summary.contains("text2"))
    }

    @Test
    fun `ask returns chat client response`() {
        val vectorStore = mock(VectorStore::class.java)
        val chatClient = mock(ChatClient::class.java, RETURNS_DEEP_STUBS)
        val chatMemory = mock(ChatMemory::class.java)

        val docs = listOf(
            Document("id1", "text1", mapOf("championName" to "A")),
            Document("id2", "text2", mapOf("championName" to "B"))
        )
        `when`(vectorStore.similaritySearch("q", 3)).thenReturn(docs)
        `when`(chatClient.prompt().advisors(any(), any()).messages(any(), any()).call().content()).thenReturn("answer")

        val bot = LolChampChatBot(chatClient, chatMemory, vectorStore)
        val result = bot.ask("s", "q")
        assertEquals("answer", result)
    }
}
