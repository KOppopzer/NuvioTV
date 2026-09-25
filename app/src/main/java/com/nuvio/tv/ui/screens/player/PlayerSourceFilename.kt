package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.domain.model.Stream
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

internal fun sourceFilenameForPlayback(
    stream: Stream,
    sourceUrl: String? = null,
    fallbackFilename: String? = null
): String? {
    stream.behaviorHints?.filename?.takeIf { it.isNotBlank() }?.let { return it }
    stream.clientResolve?.stream?.raw?.filename?.takeIf { it.isNotBlank() }?.let { return it }
    stream.clientResolve?.filename?.takeIf { it.isNotBlank() }?.let { return it }

    val pathFilename = listOfNotNull(stream.getStreamUrl(), sourceUrl)
        .distinct()
        .firstNotNullOfOrNull(::filenameFromUrl)

    return pathFilename
        ?: stream.title?.takeIf { it.isNotBlank() }
        ?: fallbackFilename?.takeIf { it.isNotBlank() }
}

private fun filenameFromUrl(url: String): String? {
    val filename = url.substringBefore('#').substringBefore('?')
        .substringAfterLast('/')
        .takeIf { it.isNotBlank() && it.contains('.') }
        ?: return null
    return runCatching {
        URLDecoder.decode(filename.replace("+", "%2B"), StandardCharsets.UTF_8.name())
    }.getOrDefault(filename)
}
