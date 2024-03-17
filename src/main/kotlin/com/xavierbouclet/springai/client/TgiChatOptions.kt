package com.xavierbouclet.springai.client

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.ai.chat.prompt.ChatOptions

object TgiChatOptions : ChatOptions {

    val DEFAULT_MODEL = "mistralai/Mistral-7B-Instruct-v0.2"

    @JsonProperty("temperature")
    var temperature: Float = 0.5f

    @JsonProperty("top_p")
    var topP: Float = 0.5f

    @JsonProperty("top_k")
    var topK=0

    var model: String = DEFAULT_MODEL
        get() = field // Custom logic can be added here
        set(value) {
            field = value // Prevent setting negative age
        }

    fun create(): TgiChatOptions {
        return TgiChatOptions
    }

    fun withModel(model: String): TgiChatOptions {
        this.model = model
        return this
    }


    override fun getTemperature(): Float {
        return temperature
    }

    override fun setTemperature(temperature: Float?) {
        this.temperature = temperature!!
    }

    override fun getTopP(): Float {
        return topP;
    }

    override fun setTopP(topP: Float?) {
        this.topP = topP!!
    }

    override fun getTopK(): Int? {
        return topK
    }

    override fun setTopK(topK: Int?) {
        this.topK = topK!!
    }

    fun toMap(): Map<String, Any> {
        return try {
            val json = ObjectMapper().writeValueAsString(this)
            ObjectMapper().readValue(json, object : TypeReference<Map<String, Any>>() {})
        } catch (e: JsonProcessingException) {
            throw RuntimeException(e)
        }
    }

    fun filterNonSupportedFields(options: Map<String, Any>): Map<String, Any> {
        return options.filterNot { (key, _) -> key == "model" }
    }


}