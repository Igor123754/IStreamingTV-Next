package com.igor.istreamingtv.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val IStreamingTypography = Typography(

    displayLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 56.sp,
        lineHeight = 64.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.5).sp
    ),

    headlineLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 38.sp,
        lineHeight = 46.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.8).sp
    ),

    headlineMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 30.sp,
        lineHeight = 38.sp,
        fontWeight = FontWeight.SemiBold
    ),

    titleLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.SemiBold
    ),

    titleMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Medium
    ),

    bodyLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 17.sp,
        lineHeight = 25.sp,
        fontWeight = FontWeight.Normal
    ),

    bodyMedium = androidx.compose.ui.text.TextStyle(
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal
    ),

    labelLarge = androidx.compose.ui.text.TextStyle(
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold
    )
)
