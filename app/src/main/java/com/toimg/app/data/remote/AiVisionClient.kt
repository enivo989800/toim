package com.toimg.app.data.remote

import com.toimg.app.domain.model.AiConfig
import com.toimg.app.domain.model.RecognitionResult
import com.toimg.app.domain.model.TestConnectionResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class ImagePayload(
    val imageId: String,
    val base64Jpeg: String,
    val fileName: String
)

class AiVisionClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    // 1x1 transparent PNG base64 for vision API test payload
    private val sampleBase64Png = "iVBORw0KGgoAAAANSU2EUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="

    suspend fun testConnection(config: AiConfig): TestConnectionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        if (config.apiKey.isBlank()) {
            return@withContext TestConnectionResult(
                isSuccess = false,
                statusCode = 0,
                latencyMs = 0,
                modelName = config.modelName,
                message = "API Key 不能为空，请配置有效 Key。"
            )
        }

        try {
            val request = if (config.providerType == AiConfig.PROVIDER_GEMINI) {
                buildGeminiTestRequest(config)
            } else {
                buildOpenAiTestRequest(config)
            }

            client.newCall(request).execute().use { response ->
                val latency = System.currentTimeMillis() - startTime
                val code = response.code
                val responseBodyStr = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    TestConnectionResult(
                        isSuccess = true,
                        statusCode = code,
                        latencyMs = latency,
                        modelName = config.modelName,
                        message = "AI 视觉模型服务连接成功！响应延迟 ${latency}ms",
                        rawResponse = responseBodyStr.take(300)
                    )
                } else {
                    val errMsg = when (code) {
                        401, 403 -> "权限验证失败 (HTTP $code)。请检查 API Key 是否有效。"
                        404 -> "模型端点不存在 (HTTP 404)。请校验 Base URL 或模型名称。"
                        429 -> "请求频率受限 (HTTP 429)。额度不足或触发 API Rate Limit。"
                        else -> "请求返回 HTTP $code 错误。$responseBodyStr"
                    }
                    TestConnectionResult(
                        isSuccess = false,
                        statusCode = code,
                        latencyMs = latency,
                        modelName = config.modelName,
                        message = errMsg,
                        rawResponse = responseBodyStr.take(300)
                    )
                }
            }
        } catch (e: Exception) {
            val latency = System.currentTimeMillis() - startTime
            TestConnectionResult(
                isSuccess = false,
                statusCode = 0,
                latencyMs = latency,
                modelName = config.modelName,
                message = "网络连接异常: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    suspend fun recognizeBatch(
        config: AiConfig,
        items: List<ImagePayload>
    ): List<RecognitionResult> = withContext(Dispatchers.IO) {
        if (items.isEmpty()) return@withContext emptyList()

        val results = mutableListOf<RecognitionResult>()
        try {
            val request = if (config.providerType == AiConfig.PROVIDER_GEMINI) {
                buildGeminiBatchRequest(config, items)
            } else {
                buildOpenAiBatchRequest(config, items)
            }

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val err = "HTTP ${response.code}: $bodyStr"
                    return@withContext items.map {
                        RecognitionResult(
                            imageId = it.imageId,
                            keywords = emptyList(),
                            description = "识别失败",
                            modelName = config.modelName,
                            isSuccess = false,
                            errorMessage = err
                        )
                    }
                }

                val parsedMap = parseVisionResponseText(bodyStr, config.providerType)
                items.forEach { payload ->
                    val recognized = parsedMap[payload.imageId]
                    if (recognized != null) {
                        results.add(recognized.copy(modelName = config.modelName))
                    } else {
                        results.add(
                            RecognitionResult(
                                imageId = payload.imageId,
                                keywords = emptyList(),
                                description = "AI 未返回该图片的分析项",
                                modelName = config.modelName,
                                isSuccess = false,
                                errorMessage = "模型响应中未找到匹配的 image_id"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            items.forEach {
                results.add(
                    RecognitionResult(
                        imageId = it.imageId,
                        keywords = emptyList(),
                        description = "请求异常",
                        modelName = config.modelName,
                        isSuccess = false,
                        errorMessage = e.localizedMessage ?: "网络或超时异常"
                    )
                )
            }
        }

        results
    }

    private fun parseVisionResponseText(
        rawHttpResponseBody: String,
        providerType: String
    ): Map<String, RecognitionResult> {
        val map = mutableMapOf<String, RecognitionResult>()
        try {
            val root = JSONObject(rawHttpResponseBody)
            val contentText: String = if (providerType == AiConfig.PROVIDER_GEMINI) {
                val candidates = root.optJSONArray("candidates")
                val content = candidates?.optJSONObject(0)?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                parts?.optJSONObject(0)?.optString("text") ?: ""
            } else {
                val choices = root.optJSONArray("choices")
                choices?.optJSONObject(0)?.optJSONObject("message")?.optString("content") ?: ""
            }

            val cleanJson = cleanJsonString(contentText)
            if (cleanJson.startsWith("[")) {
                val arr = JSONArray(cleanJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    parseItemAndPut(obj, map)
                }
            } else if (cleanJson.startsWith("{")) {
                val obj = JSONObject(cleanJson)
                if (obj.has("results")) {
                    val arr = obj.optJSONArray("results")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            parseItemAndPut(arr.optJSONObject(i) ?: continue, map)
                        }
                    }
                } else if (obj.has("images")) {
                    val arr = obj.optJSONArray("images")
                    if (arr != null) {
                        for (i in 0 until arr.length()) {
                            parseItemAndPut(arr.optJSONObject(i) ?: continue, map)
                        }
                    }
                } else {
                    parseItemAndPut(obj, map)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    private fun parseItemAndPut(obj: JSONObject, map: MutableMap<String, RecognitionResult>) {
        val id = obj.optString("image_id", "")
        if (id.isBlank()) return

        val keywordsList = mutableListOf<String>()
        val kwArr = obj.optJSONArray("keywords")
        if (kwArr != null) {
            for (k in 0 until kwArr.length()) {
                val kw = kwArr.optString(k, "").trim()
                if (kw.isNotBlank() && keywordsList.size < 10) {
                    keywordsList.add(kw)
                }
            }
        } else {
            val kwStr = obj.optString("keywords", "")
            if (kwStr.isNotBlank()) {
                kwStr.split(",", "，", " ").filter { it.isNotBlank() }.take(10).forEach {
                    keywordsList.add(it.trim())
                }
            }
        }

        val desc = obj.optString("description", "无详细描述")
        val ocr = obj.optString("ocr_text", "").ifBlank { null }
        val isSensitive = obj.optBoolean("is_sensitive", false)
        map[id] = RecognitionResult(
            imageId = id,
            keywords = keywordsList,
            description = desc,
            modelName = "",
            isSuccess = true,
            ocrText = ocr,
            isSensitive = isSensitive
        )
    }

    private fun cleanJsonString(raw: String): String {
        var str = raw.trim()
        if (str.startsWith("```json")) {
            str = str.removePrefix("```json")
        } else if (str.startsWith("```")) {
            str = str.removePrefix("```")
        }
        if (str.endsWith("```")) {
            str = str.removeSuffix("```")
        }
        return str.trim()
    }

    private fun buildGeminiBatchRequest(config: AiConfig, items: List<ImagePayload>): Request {
        val cleanBaseUrl = config.baseUrl.trimEnd('/')
        val url = if (cleanBaseUrl.contains("key=")) {
            cleanBaseUrl
        } else {
            "$cleanBaseUrl/models/${config.modelName}:generateContent?key=${config.apiKey}"
        }

        val partsArray = JSONArray()
        val instructionPrompt = StringBuilder().apply {
            append("你是一个专业的高精度图像理解与分类助手。\n")
            append("请仔细分析传入的每张图片，为每张图片返回一个 JSON 数组。数组中的每个元素包含：\n")
            append("1. image_id: 必须与每张图片标注的ID完全严格一致；\n")
            append("2. keywords: 3到10个精准中文关键词标签（如主体、场景、色调、分类）；\n")
            append("3. description: 一句简明扼要的中文总结描述；\n")
            append("4. ocr_text: 提取图片中显著可见的文字（如发票金额单号、证件文字、代码截图、招牌等，无文字则填空字符串）；\n")
            append("5. is_sensitive: 布尔值，判断是否包含身份证、银行卡、护照等敏感证件信息。\n\n")
            append("必须严格输出纯 JSON 数组，禁止任何多余修饰与 Markdown 标记。\n格式如下：\n")
            append("[{\"image_id\":\"xxx\",\"keywords\":[\"标签1\",\"标签2\"],\"description\":\"总结\",\"ocr_text\":\"...\",\"is_sensitive\":false}]")
        }.toString()

        partsArray.put(JSONObject().put("text", instructionPrompt))

        items.forEach { item ->
            partsArray.put(JSONObject().put("text", "【目标图片识别项】image_id: \"${item.imageId}\", 文件名: \"${item.fileName}\""))
            partsArray.put(
                JSONObject().apply {
                    put("inline_data", JSONObject().apply {
                        put("mime_type", "image/jpeg")
                        put("data", item.base64Jpeg)
                    })
                }
            )
        }

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().put("parts", partsArray))
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.2)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)

        return Request.Builder()
            .url(url)
            .post(body)
            .build()
    }

    private fun buildOpenAiBatchRequest(config: AiConfig, items: List<ImagePayload>): Request {
        val cleanBaseUrl = config.baseUrl.trimEnd('/')
        val url = if (cleanBaseUrl.endsWith("/chat/completions")) {
            cleanBaseUrl
        } else {
            "$cleanBaseUrl/chat/completions"
        }

        val contentArray = JSONArray()
        contentArray.put(
            JSONObject().apply {
                put("type", "text")
                put(
                    "text",
                    "请仔细分析传入的图片，输出纯 JSON 数组：[{\"image_id\":\"xxx\",\"keywords\":[\"标签1\",\"标签2\"],\"description\":\"描述\",\"ocr_text\":\"提取文字\",\"is_sensitive\":false}]。每个 image_id 必须与对应标注严格一致，keywords 提供 3-10 个中文标签，ocr_text 提取关键文本，is_sensitive 标示是否为证件或银行卡等隐私图。"
                )
            }
        )

        items.forEach { item ->
            contentArray.put(
                JSONObject().apply {
                    put("type", "text")
                    put("text", "【待分析图片】image_id: \"${item.imageId}\"")
                }
            )
            contentArray.put(
                JSONObject().apply {
                    put("type", "image_url")
                    put("image_url", JSONObject().put("url", "data:image/jpeg;base64,${item.base64Jpeg}"))
                }
            )
        }

        val jsonBody = JSONObject().apply {
            put("model", config.modelName)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", contentArray)
                })
            })
            put("temperature", 0.2)
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)

        return Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .post(body)
            .build()
    }

    private fun buildGeminiTestRequest(config: AiConfig): Request {
        val cleanBaseUrl = config.baseUrl.trimEnd('/')
        val url = if (cleanBaseUrl.contains("key=")) {
            cleanBaseUrl
        } else {
            "$cleanBaseUrl/models/${config.modelName}:generateContent?key=${config.apiKey}"
        }

        val jsonBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "Respond with JSON: {\"status\":\"ok\",\"model\":\"${config.modelName}\"}"))
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/png")
                                put("data", sampleBase64Png)
                            })
                        })
                    })
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)

        return Request.Builder()
            .url(url)
            .post(body)
            .build()
    }

    private fun buildOpenAiTestRequest(config: AiConfig): Request {
        val cleanBaseUrl = config.baseUrl.trimEnd('/')
        val url = if (cleanBaseUrl.endsWith("/chat/completions")) {
            cleanBaseUrl
        } else {
            "$cleanBaseUrl/chat/completions"
        }

        val jsonBody = JSONObject().apply {
            put("model", config.modelName)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "text")
                            put("text", "Respond with JSON: {\"status\":\"ok\"}")
                        })
                        put(JSONObject().apply {
                            put("type", "image_url")
                            put("image_url", JSONObject().put("url", "data:image/png;base64,$sampleBase64Png"))
                        })
                    })
                })
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val body = jsonBody.toString().toRequestBody(mediaType)

        return Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer ${config.apiKey}")
            .post(body)
            .build()
    }
}
