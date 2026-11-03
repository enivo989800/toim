package com.toimg.app.data.repository

import android.content.Context
import com.toimg.app.BuildConfig
import com.toimg.app.domain.model.AiConfig

class AiConfigRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("toimg_ai_settings", Context.MODE_PRIVATE)

    fun getAiConfig(): AiConfig {
        val savedKey = prefs.getString("api_key", null)
        val apiKey = if (!savedKey.isNullOrBlank()) {
            savedKey
        } else {
            try {
                val buildConfigField = BuildConfig::class.java.getField("GEMINI_API_KEY")
                (buildConfigField.get(null) as? String) ?: ""
            } catch (e: Exception) {
                ""
            }
        }

        val baseUrl = prefs.getString("base_url", AiConfig.DEFAULT_BASE_URL) ?: AiConfig.DEFAULT_BASE_URL
        val modelName = prefs.getString("model_name", AiConfig.DEFAULT_MODEL) ?: AiConfig.DEFAULT_MODEL
        val providerType = prefs.getString("provider_type", AiConfig.PROVIDER_GEMINI) ?: AiConfig.PROVIDER_GEMINI

        return AiConfig(
            apiKey = apiKey,
            baseUrl = baseUrl,
            modelName = modelName,
            providerType = providerType
        )
    }

    fun saveAiConfig(config: AiConfig) {
        prefs.edit()
            .putString("api_key", config.apiKey)
            .putString("base_url", config.baseUrl)
            .putString("model_name", config.modelName)
            .putString("provider_type", config.providerType)
            .apply()
    }

    fun clearApiKey() {
        prefs.edit().remove("api_key").apply()
    }
}
