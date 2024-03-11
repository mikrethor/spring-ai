package com.xavierbouclet.springai

import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.image.ImagePrompt
import org.springframework.ai.ollama.OllamaChatClient
import org.springframework.ai.openai.OpenAiChatClient
import org.springframework.ai.openai.OpenAiImageClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.router


@Configuration(proxyBeanMethods = false)
class RouterConfiguration {

    @Value("\${spring.ai.openai.api-key}")
    private lateinit var apiKey: String

    @Bean
    fun aiRouter(chatClient: OpenAiChatClient,
                 imageClient: OpenAiImageClient,
                 ollamaChatClient: OllamaChatClient) = router {
        GET("/api/ai/generate") { request ->
            ServerResponse
                .ok()
                .body(
                    chatClient.call(
                        request
                            .param("message")
                            .orElse("Tell me a Chuck Norris fact")
                    )
                )
        }
        GET("/api/ai/generateStream") { request ->
            ServerResponse
                .ok()
                .body(chatClient.stream(
                    Prompt(
                        UserMessage(
                            request
                                .param("message")
                                .orElse("Tell me a Chuck Norris fact")
                        )
                    )
                ).mapNotNull { chatResp -> chatResp?.result?.output?.content }
                    .toStream()
                    .toList()
                )
        }
        GET("/api/ollama/generate") { request ->
            ServerResponse
                .ok()
                .body(
                    ollamaChatClient.call(
                        request
                            .param("message")
                            .orElse("Tell me a Chuck Norris fact")
                    )
                )
        }
        GET("/api/ollama/generateStream") { request ->
            ServerResponse
                .ok()
                .body(ollamaChatClient.stream(
                    Prompt(
                        UserMessage(
                            request
                                .param("message")
                                .orElse("Tell me a Chuck Norris fact")
                        )
                    )
                ).mapNotNull { chatResp -> chatResp?.result?.output?.content }
                    .toStream()
                    .toList()
                )
        }
        GET("/api/ai/generateImage") { request ->

            ServerResponse
                .ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .body(
                    imageResponse(
                        imageClient, request
                            .param("message")
                            .orElse("A photo of a cat")
                    )
                )
        }
    }

    private fun imageResponse(imageClient: OpenAiImageClient, instruction: String): ByteArray {

        val imageResponse = imageClient.call(
            ImagePrompt(
                instruction
            )
        )
        //Could be donwloaded from the URL after the image is generated
        //val downloadUrl=imageResponse.result.output.url
        val imageInBase64 = imageResponse.result.output.b64Json


        val files = imageResponse.results.asSequence().withIndex().map {
            val test = imageInBase64.decodeFromBase64(it.value.output.b64Json)
            Pair("${it.index}.png", test)
        }.toMap()

        return ZipUtilities.createZipFile(files)
    }
}