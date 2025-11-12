package com.sun.moviedb.data.repository.source.remote.parse

import com.sun.moviedb.data.model.Category
import com.sun.moviedb.data.model.Country
import com.sun.moviedb.data.model.EpisodeWrapper
import com.sun.moviedb.data.model.Item
import com.sun.moviedb.data.model.Movie
import com.sun.moviedb.data.model.Pagination
import com.sun.moviedb.data.model.EpisodeModel
import com.sun.moviedb.utils.Constants
import org.json.JSONArray
import org.json.JSONObject

/* *
* JSON helpers (extensions)
* */
private inline fun <T> JSONArray.mapObjects(mapper: (JSONObject) -> T): List<T> {
    val out = ArrayList<T>(length())
    for (i in 0 until length()) {
        val o = optJSONObject(i) ?: continue
        out.add(mapper(o))
    }
    return out
}

private fun JSONObject.optBooleanCoerce(key: String, default: Boolean = false): Boolean {
    return when (val v = opt(key)) {
        is Boolean -> v
        is String -> v.equals("true", true) || v == "1"
        is Number -> v.toInt() != 0
        else -> default
    }
}

fun JSONObject.optStringMulti(vararg keys: String, default: String = ""): String {
    for (k in keys) {
        when (val v = this.opt(k)) {
            is String -> if (v.isNotBlank()) return v
            is Number -> return v.toString()
            is Boolean -> return v.toString()
        }
    }
    return default
}

/* *
* Model parsers (extensions)
* */
fun JSONObject.toCategory(): Category = Category(
    id = optStringMulti("id", "_id"),
    name = optString("name"),
    slug = optString("slug")
)

fun JSONArray.toCategoryList(): List<Category> = mapObjects { it.toCategory() }

fun JSONObject.toCountry(): Country = Country(
    id = optStringMulti("id", "_id"),
    name = optString("name"),
    slug = optString("slug")
)

fun JSONArray.toCountryList(): List<Country> = mapObjects { it.toCountry() }

fun JSONArray.toStringList(): List<String> {
    val out = ArrayList<String>(length())
    for (i in 0 until length()) {
        val s = optString(i, null)
        if (s != null) out.add(s)
    }
    return out
}

fun JSONObject.toServerData(): EpisodeModel = EpisodeModel(
    filename = optString("filename"),
    linkEmbed = optString("link_embed"),
    linkM3u8 = optString("link_m3u8"),
    name = optString("name"),
    slug = optString("slug")
)

fun JSONArray.toServerDataList(): List<EpisodeModel> = mapObjects { it.toServerData() }

fun JSONObject.toEpisode(): EpisodeWrapper = EpisodeWrapper(
    serverName = optString("server_name"),
    serverData = optJSONArray("server_data")?.toServerDataList() ?: emptyList()
)

fun JSONArray.toEpisodeList(): List<EpisodeWrapper> = mapObjects { it.toEpisode() }

fun JSONObject.toMovie(): Movie = Movie(
    id = optString("_id"),
    actor = optJSONArray("actor")?.toStringList() ?: emptyList(),
    category = optJSONArray("category")?.toCategoryList() ?: emptyList(),
    chieurap = optBoolean("chieurap", false),
    content = optString("content"),
    country = optJSONArray("country")?.toCountryList() ?: emptyList(),
    director = optJSONArray("director")?.toStringList() ?: emptyList(),
    episodeCurrent = optString("episode_current"),
    episodeTotal = optString("episode_total"),
    lang = optString("lang"),
    name = optString("name"),
    originName = optString("origin_name"),
    posterUrl = optString("poster_url"),
    quality = optString("quality"),
    slug = optString("slug"),
    status = optString("status"),
    thumbUrl = optString("thumb_url"),
    time = optString("time"),
    trailerUrl = optString("trailer_url"),
    type = optString("type"),
    view = optInt("view", 0),
    year = optInt("year", 0)
)

private fun String.toFullImageUrl(domain: String): String {
    return if (this.startsWith("http://") || this.startsWith("https://")) this
    else if (this.isNotBlank()) domain.trimEnd('/') + "/" + this.trimStart('/')
    else this
}

fun JSONObject.toItem(): Item = Item(
    id = optStringMulti("_id", "id"),
    category = optJSONArray("category")?.toCategoryList() ?: emptyList(),
    chieurap = optBoolean("chieurap", false),
    country = optJSONArray("country")?.toCountryList() ?: emptyList(),
    episodeCurrent = optString("episode_current"),
    lang = optString("lang"),
    name = optString("name"),
    originName = optString("origin_name"),
    posterUrl = optString("poster_url").toFullImageUrl(Constants.APP_DOMAIN_CDN_IMAGE),
    quality = optString("quality"),
    slug = optString("slug"),
    thumbUrl = optString("thumb_url").toFullImageUrl(Constants.APP_DOMAIN_CDN_IMAGE),
    time = optString("time"),
    type = optString("type"),
    year = optInt("year", 0)
)


fun JSONObject.toPagination(): Pagination = Pagination(
    totalItems = optInt("totalItems", 0),
    totalItemsPerPage = optInt("totalItemsPerPage", 0),
    currentPage = optInt("currentPage", 0),
    totalPages = optInt("totalPages", 0)
)
