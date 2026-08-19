package com.igor.istreamingtv.data.profile

import android.content.Context
import com.igor.istreamingtv.data.remote.TmdbClient
import kotlinx.serialization.encodeToString
import java.util.UUID

/**
 * ✅ Lokalno čuvanje profila (SharedPreferences + JSON).
 *    Ukupno ~1-2 KB — bez mreže, bez pozadinskih procesa.
 */
object ProfileStore {

    private const val PREFS = "istream_profiles"
    private const val KEY_LIST = "profiles_json"
    private const val KEY_LAST = "last_profile_id"

    fun load(context: Context): List<Profile> {
        val json = prefs(context).getString(KEY_LIST, null) ?: return emptyList()
        return try {
            TmdbClient.json.decodeFromString<List<Profile>>(json)
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun save(context: Context, list: List<Profile>) {
        prefs(context).edit().putString(KEY_LIST, TmdbClient.json.encodeToString(list)).apply()
    }

    fun add(context: Context, profile: Profile): List<Profile> {
        val list = load(context) + profile
        save(context, list)
        setLastUsed(context, profile.id)
        return sorted(list)
    }

    fun update(context: Context, profile: Profile): List<Profile> {
        val list = load(context).map { if (it.id == profile.id) profile else it }
        save(context, list)
        return sorted(list)
    }

    fun delete(context: Context, id: String): List<Profile> {
        val list = load(context).filterNot { it.id == id }
        save(context, list)
        return sorted(list)
    }

    fun touch(context: Context, id: String): List<Profile> {
        val list = load(context).map {
            if (it.id == id) it.copy(lastUsedAt = System.currentTimeMillis()) else it
        }
        save(context, list)
        setLastUsed(context, id)
        return sorted(list)
    }

    fun lastUsedId(context: Context): String? =
        prefs(context).getString(KEY_LAST, null)

    private fun setLastUsed(context: Context, id: String) {
        prefs(context).edit().putString(KEY_LAST, id).apply()
    }

    private fun sorted(list: List<Profile>) = list.sortedByDescending { it.lastUsedAt }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun newProfile(name: String, colorHex: String, isKids: Boolean) = Profile(
        id = UUID.randomUUID().toString(),
        name = name.trim(),
        colorHex = colorHex,
        isKids = isKids,
        createdAt = System.currentTimeMillis(),
        lastUsedAt = System.currentTimeMillis()
    )
}
