package com.xavierbouclet.springai

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.xavierbouclet.springai.client.TgiChatApi
import org.junit.jupiter.api.Test

class JacksonTest {

    @Test
    fun testJackson() {
        //val json="{\"id\":\"\",\"object\":\"text_completion\",\"created\":1710521574,\"model\":\"mistralai/Mistral-7B-Instruct-v0.2\",\"system_fingerprint\":\"1.4.0-sha-c2d4a3b\",\"choices\":[{\"index\":1,\"delta\":{\"role\":\"assistant\",\"content\":\" Chuck\"},\"logprobs\":null,\"finish_reason\":null}]}"
        val json="{\"id\":\"1\",\"object\":\"text_completion\",\"created\":1710521574,\"model\":\"mistralai/Mistral-7B-Instruct-v0.2\",\"system_fingerprint\":\"1.4.0-sha-c2d4a3b\",\"choices\":[{\"index\":1,\"delta\":{\"role\":\"assistant\",\"content\":\" Chuck\"},\"logprobs\":null,\"finish_reason\":null}]}"
        val objectMapper: ObjectMapper = jacksonObjectMapper()
        objectMapper.registerModule(JavaTimeModule())

        val chatStreamResponse: TgiChatApi.ChatStreamResponse = objectMapper.readValue(json, TgiChatApi.ChatStreamResponse::class.java)

        println(chatStreamResponse)

    }
}