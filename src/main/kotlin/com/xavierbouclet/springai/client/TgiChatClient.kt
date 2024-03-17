package com.xavierbouclet.springai.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.xavierbouclet.springai.client.TgiChatClient.TgiChatClient.logger
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.ChatClient
import org.springframework.ai.chat.ChatResponse
import org.springframework.ai.chat.Generation
import org.springframework.ai.chat.StreamingChatClient
import org.springframework.ai.chat.messages.Message
import org.springframework.ai.chat.messages.MessageType
import org.springframework.ai.chat.metadata.ChatGenerationMetadata
import org.springframework.ai.chat.metadata.Usage
import org.springframework.ai.chat.prompt.ChatOptions
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.model.ModelOptionsUtils
import org.springframework.util.StringUtils
import reactor.core.publisher.Flux
import java.util.List

class TgiChatClient(val chatApi: TgiChatApi) : ChatClient, StreamingChatClient {

    private val defaultOptions: TgiChatOptions = TgiChatOptions.create().withModel(TgiChatOptions.DEFAULT_MODEL)

    object TgiChatClient {
        val options = TgiChatOptions.create().withModel(TgiChatOptions.DEFAULT_MODEL)
        val logger = LoggerFactory.getLogger(TgiChatClient::class.java)
    }

    override fun call(prompt: Prompt?): ChatResponse {
        logger.info("Calling TGI chat with prompt: $prompt")
        val response: TgiChatApi.ChatResponse = this.chatApi.chat(tgiChatRequest(prompt!!, false))
        var generator = Generation(response.message.content)
        if (response.promptEvalCount != null && response.evalCount != null) {
            generator = generator
                .withGenerationMetadata(ChatGenerationMetadata.from("unknown", extractUsage(response)))
        }
        return ChatResponse(List.of(generator))
    }

    fun tgiChatRequest(prompt: Prompt, stream: Boolean): TgiChatApi.ChatRequest {
        val tgiMessages = prompt.instructions
            .stream()
            .filter({ message: Message ->
                (message.getMessageType() == MessageType.USER
                        ) || (message.getMessageType() == MessageType.ASSISTANT
                        ) || (message.getMessageType() == MessageType.SYSTEM)
            })
            .map({ m: Message ->
                TgiChatApi.Message(
                    toRole(m), m.getContent(), null
                )
            })
            .toList()

        // runtime options
        var runtimeOptions: TgiChatOptions? = null
        if (prompt.options != null) {
            if (prompt.options is ChatOptions) {
                runtimeOptions = ModelOptionsUtils.copyToTarget<ChatOptions, ChatOptions, TgiChatOptions>(
                    prompt.options as ChatOptions, ChatOptions::class.java,
                    TgiChatOptions::class.java
                )
            } else {
                throw IllegalArgumentException(
                    "Prompt options are not of type ChatOptions: "
                            + prompt.options.javaClass.simpleName
                )
            }
        }

        val mergedOptions = ModelOptionsUtils.merge(runtimeOptions, this.defaultOptions, TgiChatOptions::class.java)

        // Override the model.
        if (!StringUtils.hasText(mergedOptions.model)) {
            throw IllegalArgumentException("Model is not set!")
        }

        val model = mergedOptions.model
        val chatRequest = TgiChatApi.ChatRequest.builder(model)
            .withStream(stream)
            .withMessages(tgiMessages)
            // .withOptions(mergedOptions)
            .build()

        val objectMapper = ObjectMapper()

        val json = objectMapper.writeValueAsString(chatRequest)

        logger.info("Chat request: $json")

        return chatRequest
    }

    private fun toRole(message: Message): TgiChatApi.Message.Role {
        when (message.messageType) {
            MessageType.USER -> return TgiChatApi.Message.Role.USER
            MessageType.ASSISTANT -> return TgiChatApi.Message.Role.ASSISTANT
            MessageType.SYSTEM -> return TgiChatApi.Message.Role.SYSTEM
            else -> throw IllegalArgumentException("Unsupported message type: " + message.messageType)
        }
    }

    private fun extractUsage(response: TgiChatApi.ChatResponse): Usage {
        return object : Usage {
            override fun getPromptTokens(): Long {
                return response.promptEvalCount.toLong()
            }

            override fun getGenerationTokens(): Long {
                return response.evalCount.toLong()
            }
        }
    }

    override fun stream(prompt: Prompt?): Flux<ChatResponse> {
        val response = chatApi.streamingChat(tgiChatRequest(prompt!!, true))
        //val response = Flux.empty<TgiChatApi.ChatStreamResponse>()

        val objectMapper: ObjectMapper = jacksonObjectMapper()
        objectMapper.registerModule(JavaTimeModule())


        return response
            .map {
                val json = objectMapper.readValue(it, TgiChatApi.ChatStreamResponse::class.java)

                logger.info("Chat stream response: $it")

                json
            }


            .map { chunk: TgiChatApi.ChatStreamResponse ->


                val choice = chunk.choices?.get(0)
                val role = choice?.delta?.role?.let { TgiChatApi.Message.Role.Companion.forValue(it) }
                val message = role?.let { TgiChatApi.Message(it, choice.delta.content, null) }

                val generator = Generation(choice?.delta?.content ?: "No content")
                    //.withGenerationMetadata(ChatGenerationMetadata.from("unknown", extractUsage(chunk))

                ChatResponse(listOf(generator))
            }
    }
}