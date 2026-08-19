package com.igor.istreamingtv.ui.profile

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.igor.istreamingtv.data.profile.Profile
import com.igor.istreamingtv.data.profile.ProfilePresets
import com.igor.istreamingtv.data.profile.ProfileStore
import com.igor.istreamingtv.ui.components.TvFocusableButton

private val ProfileBg = Color(0xFF020204)

/**
 * ✅ "KO GLEDA?" — biranje + pravljenje profila (avatar, boja, dečiji, PIN).
 *    Profil sa PIN-om traži šifru pri izboru.
 */
@Composable
fun ProfileScreen(
    onSelected: (Profile) -> Unit,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    var profiles by remember {
        mutableStateOf(ProfileStore.load(context).sortedByDescending { it.lastUsedAt })
    }
    var creating by remember { mutableStateOf(profiles.isEmpty()) }
    var lockedProfile by remember { mutableStateOf<Profile?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(ProfileBg)) {
        when {
            creating -> ProfileForm(
                onSave = { p ->
                    profiles = ProfileStore.add(context, p)
                    creating = false
                },
                onCancel = { if (profiles.isNotEmpty()) creating = false }
            )

            else -> Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("Ko gleda?", color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(56.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    profiles.forEach { p ->
                        ProfileCard(
                            profile = p,
                            onClick = {
                                if (p.hasPin) lockedProfile = p
                                else onSelected(p)
                            },
                            onLongClick = {
                                profiles = ProfileStore.delete(context, p.id)
                            }
                        )
                    }
                    AddProfileCard { creating = true }
                }

                if (onBack != null) {
                    Spacer(modifier = Modifier.height(48.dp))
                    TvFocusableButton(onClick = onBack) { focused ->
                        Text(
                            "Nazad",
                            color = Color.White.copy(alpha = if (focused) 1f else 0.6f),
                            fontSize = 14.sp,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }

        // ✅ PIN overlay za zaključan profil
        lockedProfile?.let { lp ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
            ) {
                PinPadUI(
                    profile = lp,
                    onSuccess = {
                        lockedProfile = null
                        onSelected(lp)
                    },
                    showCancel = true,
                    onCancel = { lockedProfile = null }
                )
            }
        }
    }
}

@Composable
private fun ProfileCard(
    profile: Profile,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(200), label = "")
    val color = remember(profile.colorHex) {
        try { Color(android.graphics.Color.parseColor(profile.colorHex)) }
        catch (_: Exception) { Color(0xFF3B4252) }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(RoundedCornerShape(24.dp))
                .background(color)
                .then(if (focused) Modifier.border(4.dp, Color.White, RoundedCornerShape(24.dp)) else Modifier)
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .focusable()
                .onFocusChanged { focused = it.isFocused },
            contentAlignment = Alignment.Center
        ) {
            Text(profile.avatar.ifBlank { profile.initial }, fontSize = 52.sp)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            profile.name,
            color = if (focused) Color.White else Color.White.copy(alpha = 0.75f),
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )

        Text(
            when {
                profile.hasPin && profile.isKids -> "🔒 Dečiji"
                profile.hasPin -> "🔒 Zaštićen"
                profile.isKids -> "Dečiji"
                else -> " "
            },
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun AddProfileCard(onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.08f else 1f, tween(200), label = "")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .border(2.dp, Color.White.copy(alpha = if (focused) 0.9f else 0.3f), RoundedCornerShape(24.dp))
                .combinedClickable(onClick = onClick)
                .focusable()
                .onFocusChanged { focused = it.isFocused },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text("Dodaj profil", color = Color.White.copy(alpha = 0.75f), fontSize = 16.sp)
    }
}

/** ✅ Forma: ime + avatar + boja + dečiji + opciona šifra */
@Composable
private fun ProfileForm(
    onSave: (Profile) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var avatar by remember { mutableStateOf(ProfilePresets.avatars[0]) }
    var colorHex by remember { mutableStateOf(ProfilePresets.colors[0]) }
    var isKids by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var usePin by remember { mutableStateOf(false) }

    val previewColor = remember(colorHex) {
        try { Color(android.graphics.Color.parseColor(colorHex)) }
        catch (_: Exception) { Color(0xFF3B4252) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Novi profil", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        // Preview
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(previewColor),
            contentAlignment = Alignment.Center
        ) {
            Text(avatar, fontSize = 42.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Ime
        BasicTextField(
            value = name,
            onValueChange = { if (it.length <= 20) name = it },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier
                .width(280.dp)
                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Avatari
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfilePresets.avatars.take(6).forEach { a -> AvatarDot(a, a == avatar) { avatar = a } }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ProfilePresets.avatars.drop(6).forEach { a -> AvatarDot(a, a == avatar) { avatar = a } }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Boje
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ProfilePresets.colors.forEach { hex ->
                ColorDot(hex, hex == colorHex) { colorHex = hex }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Dečiji + PIN
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TvFocusableButton(onClick = { isKids = !isKids }) { focused ->
                ToggleChip(if (isKids) "✓ Dečiji" else "Dečiji", isKids, focused)
            }
            TvFocusableButton(onClick = { usePin = !usePin; if (!usePin) pin = "" }) { focused ->
                ToggleChip(if (usePin) "✓ Šifra" else "Šifra", usePin, focused)
            }
        }

        if (usePin) {
            Spacer(modifier = Modifier.height(14.dp))
            BasicTextField(
                value = pin,
                onValueChange = { v -> pin = v.filter { it.isDigit() }.take(4) },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 18.sp, letterSpacing = 8.sp),
                cursorBrush = SolidColor(Color.White),
                modifier = Modifier
                    .width(160.dp)
                    .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
            Text("4 cifre", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(28.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TvFocusableButton(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            ProfileStore.newProfile(
                                name = name,
                                colorHex = colorHex,
                                avatar = avatar,
                                isKids = isKids,
                                pin = if (usePin && pin.length == 4) pin else null
                            )
                        )
                    }
                }
            ) { focused ->
                Box(
                    modifier = Modifier
                        .scale(if (focused) 1.05f else 1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (name.isNotBlank()) Color.White else Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text("Sačuvaj", color = Color.Black, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            TvFocusableButton(onClick = onCancel) { focused ->
                Box(
                    modifier = Modifier
                        .scale(if (focused) 1.05f else 1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                        .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text("Odustani", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ToggleChip(label: String, active: Boolean, focused: Boolean) {
    Row(
        modifier = Modifier
            .scale(if (focused) 1.05f else 1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) Color(0xFF30D158) else Color.White.copy(alpha = 0.1f))
            .then(if (focused) Modifier.border(2.dp, Color.White, RoundedCornerShape(12.dp)) else Modifier)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun AvatarDot(avatar: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.15f else 1f, tween(150), label = "")

    Box(
        modifier = Modifier
            .size(44.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = if (selected) 0.25f else 0.08f))
            .then(
                when {
                    selected -> Modifier.border(3.dp, Color.White, CircleShape)
                    focused -> Modifier.border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    else -> Modifier
                }
            )
            .combinedClickable(onClick = onClick)
            .focusable()
            .onFocusChanged { focused = it.isFocused },
        contentAlignment = Alignment.Center
    ) {
        Text(avatar, fontSize = 22.sp)
    }
}

@Composable
private fun ColorDot(hex: String, selected: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.15f else 1f, tween(150), label = "")
    val color = remember(hex) {
        try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Gray }
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(color)
            .then(
                when {
                    selected -> Modifier.border(3.dp, Color.White, CircleShape)
                    focused -> Modifier.border(3.dp, Color.White.copy(alpha = 0.6f), CircleShape)
                    else -> Modifier
                }
            )
            .combinedClickable(onClick = onClick)
            .focusable()
            .onFocusChanged { focused = it.isFocused }
    )
}
