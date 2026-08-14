package com.igor.istreamingtv.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Stavka "Nastavi gledanje" — memoriše se u SharedPreferences (bez baze, bez keša mreže).
 */
data class ContinueEntry(
    val key: String,
    val imdbId: String?,
    val title: String,
    val posterUrl: String,
    val backdropUrl: String,
    val isTv: Boolean,
    val season: Int,
    val episode: Int,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long
)

object ContinueWatchingStore {

    private const val PREFS = "continue_watching"
    private const val KEY_ENTRIES = "entries"
    private const val LIMIT = 10

    private val gson = Gson()
    private val listType = object : TypeToken<MutableList<ContinueEntry>>() {}.type

    fun load(context: Context): List<ContinueEntry> = try {
        val json = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ENTRIES, null)
        if (json.isNullOrBlank()) emptyList()
        else gson.fromJson(json, listType) ?: emptyList()
    } catch (_: Exception) {
        emptyList()
    }

    /**
     * Odgledano do kraja → briše se; manje od 60s → ignoriše;
     * inače → upisuje na VRH liste (max 10).
     */
    fun upsert(
        context: Context,
        entry: ContinueEntry,
        positionMs: Long,
        durationMs: Long
    ) {
        if (durationMs <= 0) return
        val list = load(context).toMutableList()
        list.removeAll { it.key == entry.key }

        val finished = positionMs > durationMs - 15_000
        val tooShort = positionMs < 60_000

        if (!finished && !tooShort) {
            list.add(
                0,
                entry.copy(
                    positionMs = positionMs,
                    durationMs = durationMs,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }
        while (list.size > LIMIT) list.removeAt(list.size - 1)

        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENTRIES, gson.toJson(list))
            .apply()
    }

    /** ✅ Ručno uklanjanje (dugi prst / dugi OK) */
    fun remove(context: Context, key: String) {
        val list = load(context).toMutableList()
        list.removeAll { it.key == key }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENTRIES, gson.toJson(list))
            .apply()
    }
}
