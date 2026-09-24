package com.nuvio.tv.ui.screens.player

import android.graphics.Paint
import android.text.SpannableString
import android.text.Spanned
import android.text.style.LineHeightSpan
import android.util.DisplayMetrics
import android.util.Log
import androidx.media3.common.text.Cue
import androidx.media3.ui.SubtitleView
import java.lang.reflect.Proxy
import java.util.WeakHashMap
import kotlin.math.roundToInt

private const val OUTPUT_FIELD_NAME = "output"

private val lineSpacingStates = WeakHashMap<SubtitleView, LineSpacingState>()

internal fun SubtitleView.setLineSpacingMultiplier(multiplier: Float) {
    val state = synchronized(lineSpacingStates) {
        lineSpacingStates.getOrPut(this) { installLineSpacingOutput() }
    }
    state.multiplier = multiplier.coerceIn(0.8f, 2f)
}

private fun SubtitleView.installLineSpacingOutput(): LineSpacingState {
    return try {
        val outputField = SubtitleView::class.java.getDeclaredField(OUTPUT_FIELD_NAME)
        outputField.isAccessible = true
        val delegate = outputField.get(this)
            ?: error("SubtitleView output is null")
        val state = LineSpacingState()
        val outputInterface = delegate.javaClass.interfaces.firstOrNull {
            it.name == "androidx.media3.ui.SubtitleView\$Output"
        } ?: error("SubtitleView output interface not found")
        val wrappedOutput = Proxy.newProxyInstance(
            outputInterface.classLoader,
            arrayOf(outputInterface)
        ) { _, method, args ->
            val invocationArgs = args?.toMutableList() ?: mutableListOf()
            if (method.name == "update" && invocationArgs.isNotEmpty()) {
                invocationArgs[0] = transformCues(invocationArgs[0] as? List<*>, state.multiplier)
            }
            method.isAccessible = true
            method.invoke(delegate, *invocationArgs.toTypedArray())
        }
        outputField.set(this, wrappedOutput)
        state
    } catch (error: ReflectiveOperationException) {
        Log.e("ExoSubtitleLineSpacing", "Unable to install subtitle line-spacing hook", error)
        LineSpacingState()
    } catch (error: SecurityException) {
        Log.e("ExoSubtitleLineSpacing", "Unable to install subtitle line-spacing hook", error)
        LineSpacingState()
    } catch (error: IllegalStateException) {
        Log.e("ExoSubtitleLineSpacing", "Unable to install subtitle line-spacing hook", error)
        LineSpacingState()
    }
}

private class LineSpacingState {
    var multiplier: Float = 1f
}

private fun transformCues(cues: List<*>?, multiplier: Float): List<Cue> {
    if (cues == null || multiplier == 1f) return cues?.filterIsInstance<Cue>().orEmpty()
    return cues.filterIsInstance<Cue>().map { cue ->
        val text = cue.text
        if (text == null || text.isEmpty()) {
            cue
        } else {
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
}

private class SubtitleLineSpacingSpan(
    private val multiplier: Float
) : LineHeightSpan.WithDensity {
    override fun chooseHeight(
        text: CharSequence,
        start: Int,
        end: Int,
        spanstartv: Int,
        v: Int,
        fm: Paint.FontMetricsInt,
        density: DisplayMetrics
    ) {
        val extra = ((fm.descent - fm.ascent) * (multiplier - 1f)).roundToInt()
        fm.descent += extra
        fm.bottom += extra
    }
}
