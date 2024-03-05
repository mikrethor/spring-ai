package com.xavierbouclet.springai

import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.openai.OpenAiChatClient
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.function.ServerResponse
import org.springframework.web.servlet.function.router

@Configuration(proxyBeanMethods = false)
class RouterConfiguration {

    @Bean
    fun aiRouter(chatClient: OpenAiChatClient) = router {
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
    }
}