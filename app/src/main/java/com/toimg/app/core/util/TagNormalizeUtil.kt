package com.toimg.app.core.util

import com.toimg.app.domain.model.CategoryItem
import com.toimg.app.domain.model.ImageItem
import com.toimg.app.domain.model.RecognitionResult

object TagNormalizeUtil {

    private val synonymMap = mapOf(
        "cat" to "猫咪/宠物",
        "cats" to "猫咪/宠物",
        "猫" to "猫咪/宠物",
        "猫咪" to "猫咪/宠物",
        "dog" to "狗狗/宠物",
        "dogs" to "狗狗/宠物",
        "狗" to "狗狗/宠物",
        "宠物" to "猫咪/宠物",
        "code" to "代码与截图",
        "代码" to "代码与截图",
        "screenshot" to "代码与截图",
        "截图" to "代码与截图",
        "编程" to "代码与截图",
        "food" to "美食/餐品",
        "美食" to "美食/餐品",
        "菜肴" to "美食/餐品",
        "风景" to "自然/风景",
        "自然" to "自然/风景",
        "landscape" to "自然/风景",
        "文档" to "文档/票据",
        "票据" to "文档/票据",
        "发票" to "文档/票据",
        "收据" to "文档/票据",
        "证件" to "文档/票据",
        "人像" to "人物/合照",
        "人物" to "人物/合照",
        "自拍" to "人物/合照"
    )

    fun normalizeTag(rawTag: String): String {
        val clean = rawTag.trim()
            .replace("#", "")
            .replace(".", "")
            .replace("，", "")
            .replace(",", "")
            .lowercase()

        if (clean.isBlank()) return "其他分类"
        return synonymMap[clean] ?: rawTag.trim().take(15)
    }

    fun buildCategories(
        images: List<ImageItem>,
        recognitions: Map<String, RecognitionResult>
    ): List<CategoryItem> {
        val imageMap = images.associateBy { it.id }
        val categoryToImageIds = mutableMapOf<String, MutableList<String>>()

        recognitions.forEach { (imageId, result) ->
            if (result.isSuccess && imageMap.containsKey(imageId)) {
                result.keywords.forEach { rawKw ->
                    val normTag = normalizeTag(rawKw)
                    val list = categoryToImageIds.getOrPut(normTag) { mutableListOf() }
                    if (!list.contains(imageId)) {
                        list.add(imageId)
                    }
                }
            }
        }

        // Add uncategorized or un-recognized images under "未分类"
        val categorizedIds = categoryToImageIds.values.flatten().toSet()
        val uncategorizedIds = images.filter { !categorizedIds.contains(it.id) }.map { it.id }
        if (uncategorizedIds.isNotEmpty()) {
            categoryToImageIds["未分类/待处理"] = uncategorizedIds.toMutableList()
        }

        return categoryToImageIds.map { (tag, ids) ->
            val coverUri = ids.firstNotNullOfOrNull { id -> imageMap[id]?.uri }
            CategoryItem(
                tag = tag,
                displayName = tag,
                imageCount = ids.size,
                coverUri = coverUri,
                imageIds = ids
            )
        }.sortedByDescending { it.imageCount }
    }
}
