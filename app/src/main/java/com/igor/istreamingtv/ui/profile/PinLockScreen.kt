package com.igor.istreamingtv.ui.profile

import android.view.KeyEvent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.igor.istreamingtv.data.profile.Profile
import com.igor.istreamingtv.data.profile.ProfileStore
import com.igor.istreamingtv.ui.components.TvFocusableButton

/** ✅ Cifre sa daljinskog (0-9 i numpad) */
private fun digitFromKey(keyCode: Int): String? = when (keyCode) {
    in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> (keyCode - KeyEvent.KEYCODE_0).toString()
    in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> (keyCode - KeyEvent.KEYCODE_NUMPAD_0).toString()
    else -> null
}

/**
 * ✅ Zajednički PIN UI — koristi ga i startup zaključavanje i biranje profila.
 *    Radi na daljinski (cifre) i na tablet (tasteri na ekranu).
 */
@Composable
fun PinPadUI(
    profile: Profile,
    onSuccess: () -> Unit,
    showCancel: Boolean = false,
    onCancel: () -> Unit = {}
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    fun submit(input: String) {
        if (ProfileStore.verifyPin(profile, input)) {
            onSuccess()
        } else {
            error = true
            pin = ""
        }
    }

    fun addDigit(d: String) {
        if (pin.length < 4) {
            error = false
            pin += d
            if (pin.length == 4) submit(pin)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { e ->
                if (e.type == KeyEventType.KeyDown) {
                    digitFromKey(e.keyCode)?.let { addDigit(it); true } ?: false
                } else false
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Avatar + naslov
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        try { Color(android.graphics.Color.parseColor(profile.colorHex)) }
                        catch (_: Exception) { Color(0xFF3B4252) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(profile.avatar.ifBlank { profile.initial }, fontSize = 36.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                "Unesite šifru za ${profile.name}",
                color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Tačkice
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                repeat(4) { i ->
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (i < pin.length) Color.White
                                else if (error) Color(0xFFE50914)
                                else Color.White.copy(alpha = 0.25f)
                            )
                    )
                }
            }

            if (error) {
                Spacer(modifier = Modifier.height(10.dp))
                Text("Pogrešna šifra", color = Color(0xFFE50914), fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Mreža cifara 3x4
            val digits = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "⌫", "0", "✓")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                for (row in 0 until 4) {
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        for (col in 0 until 3) {
                            val d = digits[row * 3 + col]
                            DigitButton(
                                label = d,
                                onClick = {
                                    when (d) {
                                        "⌫" -> pin = pin.dropLast(1)
                                        "✓" -> if (pin.isNotEmpty()) submit(pin)
                                        else -> addDigit(d)
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }

            if (showCancel) {
                Spacer(modifier = Modifier.height(16.dp))
                TvFocusableButton(onClick = onCancel) { focused ->
                    Text(
                        "Otkaži",
                        color = Color.White.copy(alpha = if (focused) 1f else 0.6f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DigitButton(label: String, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (focused) 1.1f else 1f, tween(140), label = "")

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.1f))
            .then(
                if (focused) Modifier.border(2.dp, Color.White, CircleShape) else Modifier
            )
            .clickable(onClick = onClick)
            .focusable()
            .onFocusChanged { focused = it.isFocused },
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = if (focused) Color.Black else Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

/** ✅ Ceo ekran za startup zaključavanje */
@Composable
fun PinLockScreen(
    profile: Profile,
    onSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF020204))) {
        PinPadUI(
            profile = profile,
            onSuccess = onSuccess,
            showCancel = true,
            onCancel = onCancel
        )
    }
}
