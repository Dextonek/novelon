package eu.kanade.tachiyomi.ui.reader.setting

/**
 * How the novel viewer sets text: [fontSize] in sp and the line height as [lineSpacingPercent] of it.
 */
data class NovelTextSettings(
    val fontSize: Int,
    val lineSpacingPercent: Int,
)
