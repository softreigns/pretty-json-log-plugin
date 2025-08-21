package io.github.orangain.prettyjsonlog.json

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import java.io.ByteArrayInputStream

private val jsonPattern = Regex("""^\s*(\{.*})(\s*)$""")

fun parseJson(text: String): Pair<JsonNode, String>? {

     try {
        val result = jsonPattern.matchEntire(text) ?: return null

        return Pair(mapper.readTree(result.groups[1]!!.value), result.groups[2]!!.value)
    } catch (e: Exception) {
        return null
    }
}

fun getJsonNode(text: String): JsonNode? {
    return try {
        mapper.readTree(text.trim())
    } catch (e: Exception) {
        null
    }
}

fun getJson(jsonText: String): Pair<JsonNode, String>? {
    return try {
        val trimmedText = jsonText.trim()
        val node = mapper.readTree(trimmedText)
        if (jsonText.length > (8 * 1024)) {
            // If the JSON is too large, remove heavy properties to avoid performance issues
            removeHeavyProperties(node)
        }
        val trailingSpaces = jsonText.substring(trimmedText.length)
        Pair(node, trailingSpaces)
    } catch (e: JsonProcessingException) {
        null
    }
}

private fun removeHeavyProperties(node: JsonNode): JsonNode {
    if (node is ObjectNode) {
        // Remove properties that are not needed for pretty printing
        node.remove("message")
    }
    return node
}

fun extractJsonElements(input: String): List<JsonElement> {
    val result = mutableListOf<JsonElement>()
    val json = Json { ignoreUnknownKeys = true }

    var i = 0
    while (i < input.length) {
        val startChar = input[i]
        if (startChar == '[' || startChar == '{') {
            val endIndex = findMatchingJsonEnd(input, i)
            if (endIndex != -1) {
                val possibleJson = input.substring(i, endIndex + 1)
                try {
                    val parsed = json.parseToJsonElement(possibleJson)
                    result.add(parsed)
                    i = endIndex + 1
                    continue
                } catch (_: Exception) {
                    // Ignore parse failure and move on
                }
            }
        }
        i++
    }

    return result
}

fun findMatchingJsonEnd(input: String, startIndex: Int): Int {
    val stack = ArrayDeque<Char>()
    val openChar = input[startIndex]
    val closeChar = if (openChar == '{') '}' else ']'

    for (i in startIndex until input.length) {
        val c = input[i]
        when (c) {
            '{', '[' -> stack.addLast(c)
            '}', ']' -> {
                if (stack.isEmpty()) return -1
                val last = stack.removeLast()
                if ((last == '{' && c != '}') || (last == '[' && c != ']')) {
                    return -1
                }
                if (stack.isEmpty()) return i
            }
        }
    }

    return -1 // No matching closing bracket found
}

fun parseXml(xml: String): Document {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val input = ByteArrayInputStream(xml.toByteArray(Charsets.UTF_8))
    return builder.parse(input)
}
