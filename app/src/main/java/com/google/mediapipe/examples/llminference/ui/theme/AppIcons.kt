package com.google.mediapipe.examples.llminference.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {
    // 🎙️ Custom Lightweight Mic Icon
    val Mic: ImageVector
        get() = ImageVector.Builder(
            name = "CustomMic",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.White),
            strokeLineWidth = 0f
        ) {
            moveTo(12f, 14f)
            arcTo(3f, 3f, 0f, false, false, 15f, 11f)
            verticalLineTo(5f)
            arcTo(3f, 3f, 0f, false, false, 9f, 5f)
            verticalLineTo(11f)
            arcTo(3f, 3f, 0f, false, false, 12f, 14f)
            close()
            moveTo(17.3f, 11f)
            arcTo(5.3f, 5.3f, 0f, false, true, 12f, 16.3f)
            arcTo(5.3f, 5.3f, 0f, false, true, 6.7f, 11f)
            horizontalLineTo(5f)
            arcTo(7f, 7f, 0f, false, false, 11f, 17.9f)
            verticalLineTo(21f)
            horizontalLineTo(13f)
            verticalLineTo(17.9f)
            arcTo(7f, 7f, 0f, false, false, 19f, 11f)
            horizontalLineTo(17.3f)
            close()
        }.build()

    // ⏹️ Custom Lightweight Stop Icon
    val Stop: ImageVector
        get() = ImageVector.Builder(
            name = "CustomStop",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).path(
            fill = SolidColor(Color.White),
            strokeLineWidth = 0f
        ) {
            moveTo(6f, 6f)
            horizontalLineTo(18f)
            verticalLineTo(18f)
            horizontalLineTo(6f)
            close()
        }.build()
}
