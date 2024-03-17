package com.xavierbouclet.springai

import com.xavierbouclet.springai.client.TgiChatApi
import com.xavierbouclet.springai.client.TgiChatClient
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration(proxyBeanMethods = false)
class TgiConfiguration {

    @Value("\${spring.ai.tgi.url}")
    private lateinit var url: String

    @Bean
    fun tgiChatApi(): TgiChatApi {
        return TgiChatApi(baseUrl = url)
    }

    @Bean
    fun tgiChatClient(tgiChatApi: TgiChatApi): TgiChatClient {
        return TgiChatClient(tgiChatApi)
    }
}