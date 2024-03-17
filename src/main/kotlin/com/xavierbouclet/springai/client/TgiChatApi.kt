package com.xavierbouclet.springai.client

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.xavierbouclet.springai.client.TgiChatApi.Message.Role
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpResponse
import org.springframework.util.Assert
import org.springframework.util.StreamUtils
import org.springframework.web.client.ResponseErrorHandler
import org.springframework.web.client.RestClient
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.SynchronousSink
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.function.Consumer



class TgiChatApi @JvmOverloads constructor(baseUrl:String? = DEFAULT_BASE_URL,
                                          restClientBuilder:RestClient.Builder = RestClient.builder()) {
    private val responseErrorHandler:ResponseErrorHandler

    private val restClient:RestClient

    private val webClient:WebClient

    private class TgiChatResponseErrorHandler  : ResponseErrorHandler {
        @Throws(IOException::class)  override fun hasError(response:ClientHttpResponse): Boolean {
            return response.statusCode.isError
        }

        @Throws(IOException::class)  override fun handleError(response:ClientHttpResponse) {
            if (response.statusCode.isError) {
                val statusCode = response.statusCode.value()
                val statusText = response.statusText
                val message = StreamUtils.copyToString(response.body, StandardCharsets.UTF_8)
                logger.warn(String.format("[%s] %s - %s", statusCode, statusText, message))
                throw RuntimeException(String.format("[%s] %s - %s", statusCode, statusText, message))
            }
        }
    }


    init  {
        this.responseErrorHandler = TgiChatResponseErrorHandler()

        val defaultHeaders = Consumer {
                headers:HttpHeaders -> headers.contentType = MediaType.APPLICATION_JSON
            headers.accept = java.util.List.of(MediaType.APPLICATION_JSON)
        }

        this.restClient = restClientBuilder.baseUrl(baseUrl!!).defaultHeaders(defaultHeaders).build()

        this.webClient = WebClient.builder().baseUrl(baseUrl).defaultHeaders(defaultHeaders).build()
    }

    // --------------------------------------------------------------------------
    // Chat & Streaming Chat
    // --------------------------------------------------------------------------
    /**
     * Chat message object.
     *
     * @param role The role of the message of type [Role].
     * @param content The content of the message.
     * @param images The list of images to send with the message.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL) class Message (@JsonProperty("role") val role:Role,
                                                              @JsonProperty("content") val content:String?,
                                                              @JsonProperty("images") images:List<ByteArray>?) {

        /**
         * The role of the message in the conversation.
         */
        enum class Role {
            /**
             * System message type used as instructions to the model.
             */
            @JsonProperty("system") SYSTEM,
            /**
             * User message type.
             */
            @JsonProperty("user") USER,
            /**
             * Assistant message type. Usually the response from the model.
             */
            @JsonProperty("assistant") ASSISTANT;

            companion object {
                @JsonCreator
                fun forValue(value: String): Role = when (value.lowercase(Locale.getDefault())) {
                    "assistant" -> ASSISTANT
                    "system" -> SYSTEM
                    "user" -> USER
                    // Autres correspondances
                    else -> throw IllegalArgumentException("Role non reconnu : $value")
                }
            }
        }

        class MessageBuilder (private val role:Role) {
            private var content:String? = null
            private var images:List<ByteArray>? = null

            fun withContent(content:String?): MessageBuilder {
                this.content = content
                return this
            }

            fun withImages(images:List<ByteArray>?): MessageBuilder {
                this.images = images
                return this
            }

            fun build(): Message {
                return Message(role, content, images)
            }
        }




        companion object {
            fun builder(role:Role): MessageBuilder {
                return MessageBuilder(role)
            }
        }}


    @JsonInclude(JsonInclude.Include.NON_NULL)
    class GenerateRequest(
    @JsonProperty("model")  model: String,
    @JsonProperty("prompt")  prompt: String,
    @JsonProperty("format")  format: String,
    @JsonProperty("options") options: Map<String, Object> ,
    @JsonProperty("system")  system: String,
    @JsonProperty("template")  template:String,
    @JsonProperty("context")  context: List<Integer>,
    @JsonProperty("stream") var stream: Boolean,
    @JsonProperty("raw")  raw: Boolean)

    @JsonInclude(JsonInclude.Include.NON_NULL)
    class GenerateResponse(
    @JsonProperty("model")  model:String,
    @JsonProperty("created_at")  createdAt: Instant,
    @JsonProperty("response")  response:String,
    @JsonProperty("done")  done:Boolean,
    @JsonProperty("context")  context:List<Integer>,
    @JsonProperty("total_duration")  totalDuration:Duration,
    @JsonProperty("load_duration")  loadDuration:Duration,
    @JsonProperty("prompt_eval_count")  promptEvalCount:Integer,
    @JsonProperty("prompt_eval_duration")  promptEvalDuration:Duration,
    @JsonProperty("eval_count")  evalCount:Integer,
    @JsonProperty("eval_duration")  evalDuration:Duration)

    /**
     * Chat request object.
     *
     * @param model The model to use for completion.
     * @param messages The list of messages to chat with.
     * @param stream Whether to stream the response.
     * @param format The format to return the response in. Currently the only accepted
     * value is "json".
     * @param options Additional model parameters. You can use the [TgiChatOptions] builder
     * to create the options then [TgiChatOptions.toMap] to convert the options into a
     * map.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL) class ChatRequest (@field:JsonProperty("model")@param:JsonProperty("model") var model:String?,
                                                                  @field:JsonProperty("messages") var messages:List<Message>,
                                                                  @field:JsonProperty("stream") var stream:Boolean,
                                                                  @field:JsonProperty("format") var format:String?,
                                                                  @field:JsonProperty("options") var options:Map<String, Any>) {

        class ChatRequestBuilder (model:String?) {
            private val model:String?
            private var messages:List<Message> = listOf()
            private var stream = false
            private var format:String? = null
            private var options:Map<String, Any> = java.util.Map.of()

            private var frequency_penalty: Int=1

            private var logprobs: Boolean=false
            private var max_tokens: Int=32
            private var n: Int=2
            private var presence_penalty: Float=0.1F

            private var seed: Int=42

            private var temperature: Int=1

            private var top_logprobs: Int=5
            private var top_p: Float=0.95F

            private var logit_bias:List<Int> = listOf(0)

            init  {
                Assert.notNull(model, "The model can not be null.")
                this.model = model
            }

            fun withMessages(messages:List<Message>): ChatRequestBuilder {
                this.messages = messages
                return this
            }

            fun withStream(stream:Boolean): ChatRequestBuilder {
                this.stream = stream
                return this
            }

            fun withFormat(format:String?): ChatRequestBuilder {
                this.format = format
                return this
            }

            fun build(): ChatRequest {
                return ChatRequest(model, messages, stream, format, options)
            }
        }


        companion object {
            fun builder(model:String?): ChatRequestBuilder {
                return ChatRequestBuilder(model)
            }
        }}

    /**
     * Ollama chat response object.
     *
     * @param model The model name used for completion.
     * @param createdAt When the request was made.
     * @param message The response [Message] with [Message.Role.ASSISTANT].
     * @param done Whether this is the final response. For streaming response only the
     * last message is marked as done. If true, this response may be followed by another
     * response with the following, additional fields: context, prompt_eval_count,
     * prompt_eval_duration, eval_count, eval_duration.
     * @param totalDuration Time spent generating the response.
     * @param loadDuration Time spent loading the model.
     * @param promptEvalCount number of tokens in the prompt.(*)
     * @param promptEvalDuration time spent evaluating the prompt.
     * @param evalCount number of tokens in the response.
     * @param evalDuration time spent generating the response.
     * @see [Chat
     * Completion API](https://github.com/jmorganca/ollama/blob/main/docs/api.md.generate-a-chat-completion)
     *
     * @see [Ollama
     * Types](https://github.com/jmorganca/ollama/blob/main/api/types.go)
     */
    @JsonInclude(JsonInclude.Include.NON_NULL) class ChatResponse (@field:JsonProperty("model")@param:JsonProperty("model") val model:String,
                                                                   @field:JsonProperty("created") var createdAt:Instant,
                                                                   @field:JsonProperty("message") var message:Message,
                                                                   @field:JsonProperty("done") var done:Boolean,
                                                                   @field:JsonProperty("total_duration") var totalDuration:Duration,
                                                                   @field:JsonProperty("load_duration") var loadDuration:Duration,
                                                                   @field:JsonProperty("prompt_eval_count") var  promptEvalCount:Int,
                                                                   @field:JsonProperty("prompt_eval_duration") var promptEvalDuration:Duration,
                                                                   @field:JsonProperty("eval_count") var evalCount:Int,
                                                                   @field:JsonProperty("eval_duration") var evalDuration:Duration)



    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class ChatStreamResponse(
        @JsonProperty("id") val id: String?,
        @JsonProperty("object") val objectType: String?,
        @JsonProperty("created") val createdAt: Instant?,
        @JsonProperty("model") val model: String?,
        @JsonProperty("system_fingerprint") val systemFingerprint: String?,
        @JsonProperty("choices") val choices: List<Choice>?,
        @JsonProperty("message") var message: String?
    )

    //@JsonInclude(JsonInclude.Include.NON_NULL)
    data class DataChatStreamResponse(
        @JsonProperty("data") val data: ChatStreamResponse,
    )

    data class Choice(
        @JsonProperty("index") val index: Int,
        @JsonProperty("delta") val delta: Delta,
        @JsonProperty("logprobs") val logProbs: Any?, // Adjust this according to the actual type or keep it as Any? if uncertain.
        @JsonProperty("finish_reason") val finishReason: String?
    )

    data class Delta(
        @JsonProperty("role") val role: String,
        @JsonProperty("content") val content: String
    )




    /**
     * Generate the next message in a chat with a provided model.
     *
     * This is a streaming endpoint (controlled by the 'stream' request property), so
     * there will be a series of responses. The final response object will include
     * statistics and additional data from the request.
     * @param chatRequest Chat request.
     * @return Chat response.
     */
    fun chat(chatRequest:ChatRequest): ChatResponse {
        Assert.notNull(chatRequest, REQUEST_BODY_NULL_ERROR)
        Assert.isTrue(!chatRequest.stream, "Stream mode must be disabled.")

      return restClient.post()
            .uri("/v1/chat/completions")
            .body(chatRequest)
            .retrieve()
            .onStatus(this.responseErrorHandler)
            .body(ChatResponse::class.java)!!
    }

    fun generateStreaming(completionRequest:GenerateRequest): Flux<GenerateResponse?> {
        Assert.notNull(completionRequest, REQUEST_BODY_NULL_ERROR)
        Assert.isTrue(completionRequest.stream, "Request must set the steam property to true.")

        return webClient.post()
            .uri("/v1/chat/completions")
            .body(Mono.just(completionRequest), GenerateRequest::class.java)
            .retrieve()
            .bodyToFlux(GenerateResponse::class.java)
            .handle{
                    data:GenerateResponse?, sink:SynchronousSink<GenerateResponse?> -> if (logger.isTraceEnabled) {
                logger.trace(data)
            }
                sink.next(data!!)
            }
    }

    fun streamingChat(chatRequest:ChatRequest): Flux<String> {
        Assert.notNull(chatRequest, REQUEST_BODY_NULL_ERROR)
        Assert.isTrue(chatRequest.stream, "Request must set the steam property to true.")

        return webClient.post()
            .uri("/v1/chat/completions")
            .body(Mono.just(chatRequest), GenerateRequest::class.java)
            .retrieve()
            .bodyToFlux(String::class.java)
            .handle{
                    data:String, sink:SynchronousSink<String> -> if (logger.isTraceEnabled) {
                logger.trace(data)
            }
                sink.next(data)
            }
    }



    companion object {
        private val logger:Log = LogFactory.getLog(TgiChatApi::class.java)

        private const val DEFAULT_BASE_URL = "http://localhost:11434"

        const val REQUEST_BODY_NULL_ERROR:String = "The request body can not be null."
    }} // @formatter:on