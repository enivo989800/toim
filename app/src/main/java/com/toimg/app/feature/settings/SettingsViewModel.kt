package com.toimg.app.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.toimg.app.data.remote.AiVisionClient
import com.toimg.app.data.repository.AiConfigRepository
import com.toimg.app.domain.model.AiConfig
import com.toimg.app.domain.model.TestConnectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import com.toimg.app.data.local.entity.SmartRuleEntity
import com.toimg.app.data.repository.LocalImageRepositoryImpl
import com.toimg.app.domain.repository.ImageRepository

data class SettingsUiState(
    val apiKey: String = "",
    val baseUrl: String = AiConfig.DEFAULT_BASE_URL,
    val modelName: String = AiConfig.DEFAULT_MODEL,
    val providerType: String = AiConfig.PROVIDER_GEMINI,
    val isKeyVisible: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: TestConnectionResult? = null,
    val saveMessage: String? = null,
    val smartRules: List<SmartRuleEntity> = emptyList()
) {
    val maskedKeyDisplay: String
        get() {
            if (apiKey.isBlank()) return "未配置 API Key"
            if (isKeyVisible) return apiKey
            return if (apiKey.length > 8) "${apiKey.take(4)}...${apiKey.takeLast(4)}" else "********"
        }
}

class SettingsViewModel(
    private val repository: ImageRepository = LocalImageRepositoryImpl()
) : ViewModel() {

    private val client = AiVisionClient()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun loadConfig(context: Context) {
        val repo = AiConfigRepository(context)
        val config = repo.getAiConfig()
        _uiState.update {
            it.copy(
                apiKey = config.apiKey,
                baseUrl = config.baseUrl,
                modelName = config.modelName,
                providerType = config.providerType
            )
        }
        loadSmartRules(context)
    }

    fun loadSmartRules(context: Context) {
        viewModelScope.launch {
            repository.seedPresetSmartRulesIfEmpty(context)
            repository.observeSmartRules(context).collect { rules ->
                _uiState.update { it.copy(smartRules = rules) }
            }
        }
    }

    fun toggleRule(context: Context, id: Long, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateRuleStatus(context, id, isEnabled)
        }
    }

    fun addRule(context: Context, name: String, keywordTrigger: String, folderName: String) {
        viewModelScope.launch {
            val rule = SmartRuleEntity(
                name = name,
                keywordTrigger = keywordTrigger,
                targetFolderName = folderName,
                targetTreeUri = "",
                isEnabled = true
            )
            repository.saveSmartRule(context, rule)
        }
    }

    fun deleteRule(context: Context, id: Long) {
        viewModelScope.launch {
            repository.deleteSmartRule(context, id)
        }
    }


    fun updateApiKey(key: String) {
        _uiState.update { it.copy(apiKey = key, saveMessage = null) }
    }

    fun updateBaseUrl(url: String) {
        _uiState.update { it.copy(baseUrl = url, saveMessage = null) }
    }

    fun updateModelName(model: String) {
        _uiState.update { it.copy(modelName = model, saveMessage = null) }
    }

    fun updateProviderType(type: String) {
        val defaultUrl = if (type == AiConfig.PROVIDER_GEMINI) {
            AiConfig.DEFAULT_BASE_URL
        } else {
            "https://api.openai.com/v1"
        }
        val defaultModel = if (type == AiConfig.PROVIDER_GEMINI) {
            AiConfig.DEFAULT_MODEL
        } else {
            "gpt-4o-mini"
        }
        _uiState.update {
            it.copy(
                providerType = type,
                baseUrl = defaultUrl,
                modelName = defaultModel,
                saveMessage = null
            )
        }
    }

    fun toggleKeyVisibility() {
        _uiState.update { it.copy(isKeyVisible = !it.isKeyVisible) }
    }

    fun testConnectivity(context: Context) {
        viewModelScope.launch {
            _uiState.update { it.copy(isTesting = true, testResult = null) }
            val currentConfig = AiConfig(
                apiKey = _uiState.value.apiKey,
                baseUrl = _uiState.value.baseUrl,
                modelName = _uiState.value.modelName,
                providerType = _uiState.value.providerType
            )
            val result = client.testConnection(currentConfig)
            _uiState.update { it.copy(isTesting = false, testResult = result) }
        }
    }

    fun saveSettings(context: Context) {
        val repo = AiConfigRepository(context)
        val config = AiConfig(
            apiKey = _uiState.value.apiKey,
            baseUrl = _uiState.value.baseUrl,
            modelName = _uiState.value.modelName,
            providerType = _uiState.value.providerType
        )
        repo.saveAiConfig(config)
        _uiState.update { it.copy(saveMessage = "AI 配置已成功保存！") }
    }

    fun clearApiKey(context: Context) {
        val repo = AiConfigRepository(context)
        repo.clearApiKey()
        _uiState.update { it.copy(apiKey = "", saveMessage = "API Key 已擦除。") }
    }
}
