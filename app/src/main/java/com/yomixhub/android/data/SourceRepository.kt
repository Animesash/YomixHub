package com.yomixhub.android.data

import com.yomixhub.android.data.source.LibGroupSource
import com.yomixhub.android.data.source.Source

/**
 * Entry point for all external sources of the app.
 * Holds singletons so their in-memory caches survive screen changes.
 *
 * All three LibGroup sites share one JSON API and are told apart by site id:
 * MangaLib = 1, RanobeLib = 3, HentaiLib = 4.
 */
object SourceRepository {

    val mangaLib: Source = LibGroupSource(
        id = "mangalib",
        name = "MangaLib",
        baseUrl = "https://mangalib.me",
        siteId = 1,
    )

    val ranobeLib: Source = LibGroupSource(
        id = "ranobelib",
        name = "RanobeLib",
        baseUrl = "https://ranobelib.me",
        siteId = 3,
    )

    val hentaiLib: Source = LibGroupSource(
        id = "hentailib",
        name = "HentaiLib",
        baseUrl = "https://hentailib.me",
        siteId = 4,
    )

    val all: List<Source> = listOf(mangaLib, ranobeLib, hentaiLib)

    fun byId(id: String?): Source? = all.firstOrNull { it.id == id }
}
