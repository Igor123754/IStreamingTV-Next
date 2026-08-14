package com.igor.istreamingtv.data

import android.content.Context
import com.igor.istreamingtv.data.remote.TmdbClient.json
import kotlinx.serialization.Serializable

@Serializable
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

    fun load(context: Context): List<ContinueEntry> = try {
        val jsonString = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ENTRIES, null)
        if (jsonString.isNullOrBlank()) emptyList()
        else json.decodeFromString<List<ContinueEntry>>(jsonString)
    } catch (_: Exception) {
        emptyList()
    }

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

        val jsonString = json.encodeToString<List<ContinueEntry>>(
            kotlinx.serialization.builtins.ListSerializer(ContinueEntry.serializer()),
            list
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENTRIES, jsonString)
            .apply()
    }

    fun remove(context: Context, key: String) {
        val list = load(context).toMutableList()
        list.removeAll { it.key == key }

        val jsonString = json.encodeToString<List<ContinueEntry>>(
            kotlinx.serialization.builtins.ListSerializer(ContinueEntry.serializer()),
            list
        )
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ENTRIES, jsonString)
            .apply()
    }
}
