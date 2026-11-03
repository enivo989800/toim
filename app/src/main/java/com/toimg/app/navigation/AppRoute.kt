package com.toimg.app.navigation

object AppRoute {
    const val HOME = "home"
    const val PICKER = "picker"
    const val WORKSPACE = "workspace"
    const val RESULT = "result"
    const val SETTINGS = "settings"

    fun titleFor(route: String?): String = when (route) {
        PICKER -> "选择图片"
        WORKSPACE -> "图片工作区"
        RESULT -> "处理结果"
        SETTINGS -> "设置"
        else -> "ToImg"
    }
}
