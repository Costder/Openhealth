package com.openhealthbridge.data.sync

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class DirectUploadClient {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun uploadBundle(
        hostUrl: String,
        token: String,
        manifestJson: String,
        payloadCiphertextB64: String
    ): Result<String> {
        val url = hostUrl.removeSuffix("/") + "/v1/direct-upload"
        val payload = """
            {
                "manifest": $manifestJson,
                "payloadCiphertextB64": "$payloadCiphertextB64"
            }
        """.trimIndent()
        
        return runCatching {
            val response = client.post(url) {
                contentType(ContentType.Application.Json)
                header("Authorization", "Bearer $token")
                setBody(payload)
            }
            val responseText = response.bodyAsText()
            val jsonResponse = Json.parseToJsonElement(responseText).jsonObject
            if (jsonResponse["ok"]?.jsonPrimitive?.booleanOrNull == true) {
                jsonResponse["bundleId"]?.jsonPrimitive?.content ?: error("Missing bundleId in response")
            } else {
                error("Upload failed: $responseText")
            }
        }
    }
}