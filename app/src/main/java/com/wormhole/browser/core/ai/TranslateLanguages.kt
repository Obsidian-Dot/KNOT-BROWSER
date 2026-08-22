package com.wormhole.browser.core.ai

data class TranslateLanguage(val code: String, val displayName: String)

object TranslateLanguages {
    val ALL: List<TranslateLanguage> = listOf(
        TranslateLanguage("en", "English"),
        TranslateLanguage("es", "Spanish"),
        TranslateLanguage("fr", "French"),
        TranslateLanguage("de", "German"),
        TranslateLanguage("it", "Italian"),
        TranslateLanguage("pt", "Portuguese"),
        TranslateLanguage("nl", "Dutch"),
        TranslateLanguage("ru", "Russian"),
        TranslateLanguage("tr", "Turkish"),
        TranslateLanguage("ar", "Arabic"),
        TranslateLanguage("hi", "Hindi"),
        TranslateLanguage("ur", "Urdu"),
        TranslateLanguage("bn", "Bengali"),
        TranslateLanguage("zh", "Chinese (Simplified)"),
        TranslateLanguage("ja", "Japanese"),
        TranslateLanguage("ko", "Korean"),
        TranslateLanguage("vi", "Vietnamese"),
        TranslateLanguage("id", "Indonesian"),
        TranslateLanguage("pl", "Polish"),
        TranslateLanguage("sv", "Swedish"),
    )
}
