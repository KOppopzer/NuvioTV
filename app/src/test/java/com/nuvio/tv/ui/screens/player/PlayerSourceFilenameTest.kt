package com.nuvio.tv.ui.screens.player

import com.nuvio.tv.domain.model.Stream
import com.nuvio.tv.domain.model.StreamBehaviorHints
import com.nuvio.tv.domain.model.StreamClientResolve
import com.nuvio.tv.domain.model.StreamClientResolveRaw
import com.nuvio.tv.domain.model.StreamClientResolveStream
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerSourceFilenameTest {

    @Test
    fun `prefers addon behavior hint filename`() {
        val stream = stream(
            url = "https://media.example/movie.mkv",
            filename = "release-from-hint.mkv"
        )

        assertEquals("release-from-hint.mkv", sourceFilenameForPlayback(stream))
    }

    @Test
    fun `uses client resolve file name for torrent streams`() {
        val stream = stream(
            clientResolve = StreamClientResolve(
                type = null,
                infoHash = null,
                fileIdx = null,
                magnetUri = null,
                sources = null,
                torrentName = null,
                filename = "fallback.mkv",
                mediaType = null,
                mediaId = null,
                mediaOnlyId = null,
                title = null,
                season = null,
                episode = null,
                service = null,
                serviceIndex = null,
                serviceExtension = null,
                isCached = null,
                stream = StreamClientResolveStream(
                    raw = StreamClientResolveRaw(
                        torrentName = null,
                        filename = "resolved-file.mkv",
                        size = null,
                        folderSize = null,
                        tracker = null,
                        indexer = null,
                        network = null,
                        parsed = null
                    )
                )
            )
        )

        assertEquals("resolved-file.mkv", sourceFilenameForPlayback(stream))
    }

    @Test
    fun `derives filename from selected stream url and decodes escaped characters`() {
        val stream = stream(url = "https://media.example/path/Star.Wars%20WEB-DL.mkv?token=secret")

        assertEquals("Star.Wars WEB-DL.mkv", sourceFilenameForPlayback(stream))
    }

    @Test
    fun `uses stream title when source url does not expose a filename`() {
        val stream = stream(
            url = "https://media.example/play?id=123",
            title = "Star.Wars.2026.1080p.WEB-DL"
        )

        assertEquals("Star.Wars.2026.1080p.WEB-DL", sourceFilenameForPlayback(stream))
    }

    @Test
    fun `uses playback url filename when addon url is opaque`() {
        val stream = stream(url = "https://addon.example/play?id=123", title = "Display title")

        assertEquals(
            "resolved-release.mkv",
            sourceFilenameForPlayback(stream, sourceUrl = "https://cdn.example/resolved-release.mkv")
        )
    }

    @Test
    fun `prefers source url filename over stream title`() {
        val stream = stream(
            url = "https://media.example/path/actual-release.mkv",
            title = "Display title"
        )

        assertEquals("actual-release.mkv", sourceFilenameForPlayback(stream))
    }

    @Test
    fun `uses navigation filename when source has no file name`() {
        assertEquals(
            "navigation-name.mkv",
            sourceFilenameForPlayback(stream(), fallbackFilename = "navigation-name.mkv")
        )
    }

    private fun stream(
        url: String? = null,
        filename: String? = null,
        title: String? = null,
        clientResolve: StreamClientResolve? = null
    ) = Stream(
        name = null,
        title = title,
        description = null,
        url = url,
        ytId = null,
        infoHash = null,
        fileIdx = null,
        externalUrl = null,
        behaviorHints = filename?.let {
            StreamBehaviorHints(
                notWebReady = null,
                bingeGroup = null,
                countryWhitelist = null,
                proxyHeaders = null,
                filename = it
            )
        },
        addonName = "test",
        addonLogo = null,
        clientResolve = clientResolve
    )
}
