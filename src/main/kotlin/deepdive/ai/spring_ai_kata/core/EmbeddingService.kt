package deepdive.ai.spring_ai_kata.core

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Service
class EmbeddingService(
    private val vectorStore: VectorStore
) {

    fun init() {
        val resourcePattern = "champion"
        val loader = javaClass.classLoader
        val files = loader.getResources(resourcePattern).toList().flatMap { url ->
            File(url.toURI()).listFiles()?.toList() ?: emptyList()
        }

        val documents = files.filter { it.name.endsWith(".json") }.flatMap { file ->
            val jsonContent = file.readText()
            // Parse JSON and extract relevant champion fields
            val championDocs = mutableListOf<Document>()
            try {
                val mapper = jacksonObjectMapper()
                val root = mapper.readTree(jsonContent)
                val dataNode = root.get("data")
                if (dataNode != null && dataNode.fieldNames().hasNext()) {
                    val champKey = dataNode.fieldNames().next()
                    val champNode = dataNode.get(champKey)
                    val name = champNode.get("name")?.asText() ?: champKey
                    val title = champNode.get("title")?.asText() ?: ""
                    val lore = champNode.get("lore")?.asText() ?: ""
                    val tags = champNode.get("tags")?.map { it.asText() }?.joinToString(", ") ?: ""
                    val metadata = mapOf("name" to name, "title" to title, "tag" to tags)

                    println("Processing champion: $name")
                    println("Title: $title")
                    println("Lore: $lore")
                    println("Tag: $tags")

                    championDocs.add(
                        Document(
                            "$name-title-lore",
                            "챔피언 이름: $name\n타이틀: $title\n배경 이야기: $lore, \n태그: $tags",
                            metadata
                        )
                    )
                }
            } catch (e: Exception) {
            }
            championDocs
        }

        vectorStore.add(documents)
    }
}