package deepdive.ai.spring_ai_kata.core

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.File
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Service
class EmbeddingService(
    private val vectorStore: VectorStore
) {
    private val nameToChampId = mutableMapOf<String, String>()
    private val champIdToKoName = mutableMapOf<String, String>()

    fun init() {
        val resourcePattern = "champion"
        val loader = javaClass.classLoader
        val files = loader.getResources(resourcePattern).toList().flatMap { url ->
            File(url.toURI()).listFiles()?.toList() ?: emptyList()
        }

        val documents = files.filter { it.name.endsWith(".json") }.flatMap { file ->
            val jsonContent = file.readText()
            val championDocs = mutableListOf<Document>()
            try {
                val mapper = jacksonObjectMapper()
                val root = mapper.readTree(jsonContent)
                val dataNode = root.get("data")
                if (dataNode != null) {
                    val fields = dataNode.fields()
                    while (fields.hasNext()) {
                        val entry = fields.next()
                        val champKey = entry.key // e.g., "Aatrox"
                        val champNode = entry.value

                        val nameKo = champNode.get("name")?.asText() ?: champKey
                        // build name maps for champion detection (ko + en)
                        nameToChampId[nameKo.lowercase()] = champKey
                        nameToChampId[champKey.lowercase()] = champKey
                        champIdToKoName[champKey] = nameKo
                        val title = champNode.get("title")?.asText().orEmpty()
                        val loreRaw = champNode.get("lore")?.asText().orEmpty()
                        val tags = champNode.get("tags")?.joinToString(", ") ?: ""

                        val baseMeta = mutableMapOf<String, Any>(
                            "champ_id" to champKey,
                            "name" to nameKo,
                            "title" to title,
                            "lang" to "ko",
                            "tags" to tags
                        )

                        // 1) lore -> sentence-based chunks with overlap
                        val lore = stripTagsAndTemplates(loreRaw)
                        val loreChunks = chunkBySentence(text = lore, maxChars = 1200, overlapChars = 120)
                        loreChunks.forEachIndexed { idx, chunk ->
                            val meta = baseMeta + mapOf("section" to "lore", "section_id" to "lore_$idx")
                            val anchored = "[챔피언: ${nameKo}][섹션: lore][id: ${champKey}]\n" + chunk
                            championDocs += Document(
                                "${champKey}::lore::${idx}",
                                anchored,
                                meta
                            )
                        }

                        // 2) allytips -> one document per tip
                        champNode.get("allytips")?.map { stripTagsAndTemplates(it.asText()) }?.forEachIndexed { i, tip ->
                            val meta = baseMeta + mapOf("section" to "allytips", "section_id" to "ally_${i}")
                            val anchored = "[챔피언: ${nameKo}][섹션: allytips][id: ${champKey}]\n" + tip
                            championDocs += Document(
                                "${champKey}::allytips::${i}",
                                anchored,
                                meta
                            )
                        }

                        // 3) enemytips -> one document per tip
                        champNode.get("enemytips")?.map { stripTagsAndTemplates(it.asText()) }?.forEachIndexed { i, tip ->
                            val meta = baseMeta + mapOf("section" to "enemytips", "section_id" to "enemy_${i}")
                            val anchored = "[챔피언: ${nameKo}][섹션: enemytips][id: ${champKey}]\n" + tip
                            championDocs += Document(
                                "${champKey}::enemytips::${i}",
                                anchored,
                                meta
                            )
                        }

                        // 4) spells -> one document per spell (description/tooltip cleaned)
                        champNode.get("spells")?.forEach { spellNode ->
                            val spellId = spellNode.get("id")?.asText() ?: "spell"
                            val spellName = spellNode.get("name")?.asText().orEmpty()
                            val desc = stripTagsAndTemplates(spellNode.get("description")?.asText().orEmpty())
                            val tooltip = stripTagsAndTemplates(spellNode.get("tooltip")?.asText().orEmpty())
                            val body = buildString {
                                appendLine(spellName)
                                if (desc.isNotBlank()) appendLine(desc)
                                if (tooltip.isNotBlank()) appendLine(tooltip)
                            }.trim()
                            val content = "[챔피언: ${nameKo}][섹션: spell][id: ${champKey}]\n" + body
                            val meta = baseMeta + mapOf("section" to "spell", "section_id" to spellId)
                            championDocs += Document(
                                "${champKey}::spell::${spellId}",
                                content,
                                meta
                            )
                        }

                        println("Processed champion: $champKey / $nameKo -> docs so far: ${championDocs.size}")
                    }
                }
            } catch (e: Exception) {
                System.err.println("Failed to process file ${file.name}: ${e.message}")
            }
            championDocs
        }

        vectorStore.add(documents)
    }

    private fun stripTagsAndTemplates(text: String): String {
        if (text.isBlank()) return text
        return text
            .replace(Regex("<[^>]+>"), " ")        // remove HTML-like tags
            .replace(Regex("\\{\\{[^}]+}}"), " ") // remove template variables like {{ var }}
            .replace(Regex("\n+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun chunkBySentence(text: String, maxChars: Int, overlapChars: Int): List<String> {
        if (text.isBlank()) return emptyList()
        val sentences = text.split(Regex("(?<=['.!?…])\\s+|(?<=[。！？])\\s+|\n+"))
        val chunks = mutableListOf<String>()
        var current = StringBuilder()

        fun flush() {
            if (current.isNotEmpty()) {
                chunks.add(current.toString().trim())
                current = StringBuilder()
            }
        }

        for (s in sentences) {
            val candidate = if (current.isEmpty()) s else current.toString() + " " + s
            if (candidate.length > maxChars) {
                flush()
                if (s.length > maxChars) {
                    var start = 0
                    while (start < s.length) {
                        val end = (start + maxChars).coerceAtMost(s.length)
                        chunks.add(s.substring(start, end).trim())
                        start = (end - overlapChars).coerceAtLeast(end) // avoid infinite loop when overlap is large
                    }
                } else {
                    current.append(s)
                }
            } else {
                if (current.isNotEmpty()) current.append(' ')
                current.append(s)
            }
        }
        flush()

        if (overlapChars <= 0 || chunks.size <= 1) return chunks

        val overlapped = mutableListOf<String>()
        for (i in chunks.indices) {
            if (i == 0) {
                overlapped.add(chunks[i])
            } else {
                val prevTail = chunks[i - 1].takeLast(overlapChars)
                overlapped.add((prevTail + " " + chunks[i]).trim())
            }
        }
        return overlapped
    }
}