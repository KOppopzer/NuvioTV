package com.nuvio.tv.ui.screens.player

import android.graphics.Paint
import android.text.SpannableString
import android.text.Spanned
import android.text.style.LineHeightSpan
import androidx.media3.common.text.Cue
import kotlin.math.roundToInt

internal fun List<Cue>.withSubtitleLineSpacing(lineSpacing: Int): List<Cue> {
    if (lineSpacing == 100) return this
    val multiplier = (lineSpacing / 100f).coerceIn(0.8f, 2f)
    return map { cue ->
        val text = cue.text ?: return@map cue
        if (text.isEmpty()) return@map cue

        val styledText = SpannableString(text)
        styledText.setSpan(
            SubtitleLineSpacingSpan(multiplier),
            0,
            styledText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        cue.buildUpon().setText(styledText).build()
    }
}

private class SubtitleLineSpacingSpan(
    private val multiplier: Float
) : LineHeightSpan {
    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanstartv: Int,
        v: Int,
        fm: Paint.FontMetricsInt
    ) {
        val extra = ((fm.descent - fm.ascent) * (multiplier - 1f)).roundToInt()
        // Keep the bottom line anchored to the existing subtitle position. Extra
        // height is added above each line so only the lines above it move.
        fm.ascent -= extra
        fm.top -= extra
    }
}
