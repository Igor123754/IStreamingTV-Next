package com.igor.istreamingtv.data.profile

import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val name: String,
    val colorHex: String,
    val avatar: String = "",
    val isKids: Boolean = false,
    val pinHash: String? = null,
    val createdAt: Long = 0L,
    val lastUsedAt: Long = 0L
) {
    val hasPin: Boolean get() = !pinHash.isNullOrBlank()
    val initial: String get() = name.trim().firstOrNull()?.uppercase() ?: "?"
}

/** ✅ Trenutno izabran profil — samo u RAM-u */
object AppSession {
    var currentProfile: Profile? = null
}

object ProfilePresets {
    val colors = listOf(
        "#E50914", "#0A84FF", "#30D158", "#FF9F0A",
        "#BF5AF2", "#64D2FF", "#FF375F", "#FFD60A"
    )
    val avatars = listOf(
        "😀", "😎", "🦁", "🐼", "", "⭐",
        "⚽", "", "", "🌈", "🐶", ""
    )
}
