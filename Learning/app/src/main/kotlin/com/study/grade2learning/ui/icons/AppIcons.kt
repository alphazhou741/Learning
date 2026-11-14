package com.study.grade2learning.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object AppIcons {

    private fun strokeIcon(
        name: String,
        block: ImageVector.Builder.() -> Unit
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(block).build()

    private fun ImageVector.Builder.strokePath(
        strokeWidth: Float = 2f,
        pathData: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit
    ) {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = pathData
        )
    }

    val Home: ImageVector by lazy {
        strokeIcon("Home") {
            strokePath {
                moveTo(3f, 10.5f)
                lineTo(12f, 3f)
                lineTo(21f, 10.5f)
                verticalLineTo(19f)
                curveTo(21f, 20.1f, 20.1f, 21f, 19f, 21f)
                horizontalLineTo(15f)
                verticalLineTo(16f)
                curveTo(15f, 14.34f, 13.66f, 13f, 12f, 13f)
                curveTo(10.34f, 13f, 9f, 14.34f, 9f, 16f)
                verticalLineTo(21f)
                horizontalLineTo(5f)
                curveTo(3.9f, 21f, 3f, 20.1f, 3f, 19f)
                close()
            }
        }
    }

    val Units: ImageVector by lazy {
        strokeIcon("Units") {
            strokePath {
                moveTo(4.2f, 4.5f)
                horizontalLineTo(19.8f)
                curveTo(20.46f, 4.5f, 21f, 5.04f, 21f, 5.7f)
                verticalLineTo(18.3f)
                curveTo(21f, 18.96f, 20.46f, 19.5f, 19.8f, 19.5f)
                horizontalLineTo(4.2f)
                curveTo(3.54f, 19.5f, 3f, 18.96f, 3f, 18.3f)
                verticalLineTo(5.7f)
                curveTo(3f, 5.04f, 3.54f, 4.5f, 4.2f, 4.5f)
                close()
                moveTo(9f, 4.5f)
                verticalLineTo(19.5f)
                moveTo(15f, 4.5f)
                verticalLineTo(19.5f)
            }
        }
    }

    val Pen: ImageVector by lazy {
        strokeIcon("Pen") {
            strokePath {
                moveTo(16.86f, 4.49f)
                lineTo(18.55f, 2.8f)
                curveTo(19.28f, 2.07f, 20.47f, 2.07f, 21.2f, 2.8f)
                curveTo(21.93f, 3.53f, 21.93f, 4.72f, 21.2f, 5.45f)
                lineTo(6.83f, 19.82f)
                lineTo(3f, 21f)
                lineTo(4.18f, 17.17f)
                close()
            }
        }
    }

    val Report: ImageVector by lazy {
        strokeIcon("Report") {
            strokePath {
                moveTo(4f, 20f)
                verticalLineTo(13f)
                horizontalLineTo(7.5f)
                verticalLineTo(20f)
                close()
                moveTo(10.25f, 20f)
                verticalLineTo(9f)
                horizontalLineTo(13.75f)
                verticalLineTo(20f)
                close()
                moveTo(16.5f, 20f)
                verticalLineTo(4.5f)
                horizontalLineTo(20f)
                verticalLineTo(20f)
                close()
            }
        }
    }

    val Lock: ImageVector by lazy {
        strokeIcon("Lock") {
            strokePath {
                moveTo(7.5f, 10.5f)
                verticalLineTo(6.75f)
                curveTo(7.5f, 4.26f, 9.51f, 2.25f, 12f, 2.25f)
                curveTo(14.49f, 2.25f, 16.5f, 4.26f, 16.5f, 6.75f)
                verticalLineTo(10.5f)
                moveTo(6.75f, 10.5f)
                horizontalLineTo(17.25f)
                curveTo(18.49f, 10.5f, 19.5f, 11.51f, 19.5f, 12.75f)
                verticalLineTo(19.5f)
                curveTo(19.5f, 20.74f, 18.49f, 21.75f, 17.25f, 21.75f)
                horizontalLineTo(6.75f)
                curveTo(5.51f, 21.75f, 4.5f, 20.74f, 4.5f, 19.5f)
                verticalLineTo(12.75f)
                curveTo(4.5f, 11.51f, 5.51f, 10.5f, 6.75f, 10.5f)
                close()
            }
        }
    }

    val Shield: ImageVector by lazy {
        strokeIcon("Shield") {
            strokePath {
                moveTo(12f, 3f)
                lineTo(4.5f, 6.2f)
                verticalLineTo(11.5f)
                curveTo(4.5f, 16.35f, 7.7f, 20.6f, 12f, 21.8f)
                curveTo(16.3f, 20.6f, 19.5f, 16.35f, 19.5f, 11.5f)
                verticalLineTo(6.2f)
                close()
                moveTo(9f, 12.5f)
                lineTo(11.1f, 14.6f)
                lineTo(15.2f, 10f)
            }
        }
    }

    val Calendar: ImageVector by lazy {
        strokeIcon("Calendar") {
            strokePath {
                moveTo(5f, 5f)
                horizontalLineTo(19f)
                curveTo(20.1f, 5f, 21f, 5.9f, 21f, 7f)
                verticalLineTo(19f)
                curveTo(21f, 20.1f, 20.1f, 21f, 19f, 21f)
                horizontalLineTo(5f)
                curveTo(3.9f, 21f, 3f, 20.1f, 3f, 19f)
                verticalLineTo(7f)
                curveTo(3f, 5.9f, 3.9f, 5f, 5f, 5f)
                close()
                moveTo(8f, 3f)
                verticalLineTo(7f)
                moveTo(16f, 3f)
                verticalLineTo(7f)
                moveTo(3f, 10f)
                horizontalLineTo(21f)
            }
        }
    }

    val Target: ImageVector by lazy {
        strokeIcon("Target") {
            strokePath {
                moveTo(20f, 12f)
                arcTo(8f, 8f, 0f, true, true, 4f, 12f)
                arcTo(8f, 8f, 0f, true, true, 20f, 12f)
                moveTo(16f, 12f)
                arcTo(4f, 4f, 0f, true, true, 8f, 12f)
                arcTo(4f, 4f, 0f, true, true, 16f, 12f)
            }
            path(
                fill = SolidColor(Color.Black),
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(13.3f, 12f)
                arcTo(1.3f, 1.3f, 0f, true, true, 10.7f, 12f)
                arcTo(1.3f, 1.3f, 0f, true, true, 13.3f, 12f)
            }
        }
    }

    val MathSymbol: ImageVector by lazy {
        strokeIcon("MathSymbol") {
            strokePath {
                moveTo(5f, 7f)
                horizontalLineTo(9f)
                moveTo(7f, 5f)
                verticalLineTo(9f)
                moveTo(15f, 7f)
                horizontalLineTo(19f)
                moveTo(5f, 15f)
                lineTo(9f, 19f)
                moveTo(9f, 15f)
                lineTo(5f, 19f)
                moveTo(15f, 15.5f)
                horizontalLineTo(19f)
                moveTo(15f, 18.5f)
                horizontalLineTo(19f)
            }
        }
    }

    val Book: ImageVector by lazy {
        strokeIcon("Book") {
            strokePath {
                moveTo(12f, 6.25f)
                verticalLineTo(19.25f)
                moveTo(12f, 6.25f)
                curveTo(10.83f, 5.48f, 9.25f, 5f, 7.5f, 5f)
                curveTo(5.75f, 5f, 4.17f, 5.48f, 3f, 6.25f)
                verticalLineTo(19.25f)
                curveTo(4.17f, 18.48f, 5.75f, 18f, 7.5f, 18f)
                curveTo(9.25f, 18f, 10.83f, 18.48f, 12f, 19.25f)
                moveTo(12f, 6.25f)
                curveTo(13.17f, 5.48f, 14.75f, 5f, 16.5f, 5f)
                curveTo(18.25f, 5f, 19.83f, 5.48f, 21f, 6.25f)
                verticalLineTo(19.25f)
                curveTo(19.83f, 18.48f, 18.25f, 18f, 16.5f, 18f)
                curveTo(14.75f, 18f, 13.17f, 18.48f, 12f, 19.25f)
            }
        }
    }

    val EnglishAa: ImageVector by lazy {
        strokeIcon("EnglishAa") {
            strokePath {
                moveTo(3.5f, 17f)
                lineTo(7.5f, 6.5f)
                lineTo(11.5f, 17f)
                moveTo(5f, 13.5f)
                horizontalLineTo(10f)
                moveTo(19.5f, 14.2f)
                arcTo(2.8f, 2.8f, 0f, true, true, 13.9f, 14.2f)
                arcTo(2.8f, 2.8f, 0f, true, true, 19.5f, 14.2f)
                moveTo(19.5f, 11.4f)
                verticalLineTo(17f)
            }
        }
    }

    val Speaker: ImageVector by lazy {
        strokeIcon("Speaker") {
            strokePath {
                moveTo(11f, 5f)
                lineTo(6f, 9f)
                horizontalLineTo(3f)
                verticalLineTo(15f)
                horizontalLineTo(6f)
                lineTo(11f, 19f)
                close()
                moveTo(15.5f, 8.5f)
                curveTo(16.9f, 9.9f, 16.9f, 14.1f, 15.5f, 15.5f)
                moveTo(18.3f, 6.2f)
                curveTo(20.9f, 8.8f, 20.9f, 15.2f, 18.3f, 17.8f)
            }
        }
    }

    val Pulse: ImageVector by lazy {
        strokeIcon("Pulse") {
            strokePath(strokeWidth = 2.2f) {
                moveTo(3f, 12f)
                horizontalLineTo(7f)
                lineTo(9.5f, 6f)
                lineTo(13.5f, 18f)
                lineTo(16f, 12f)
                horizontalLineTo(21f)
            }
        }
    }

    val Bulb: ImageVector by lazy {
        strokeIcon("Bulb") {
            strokePath {
                moveTo(9f, 18f)
                horizontalLineTo(15f)
                moveTo(10f, 21f)
                horizontalLineTo(14f)
                moveTo(9.5f, 15f)
                verticalLineTo(13.7f)
                curveTo(7.4f, 12.6f, 6f, 10.4f, 6f, 8.8f)
                curveTo(6f, 5.5f, 8.7f, 3f, 12f, 3f)
                curveTo(15.3f, 3f, 18f, 5.5f, 18f, 8.8f)
                curveTo(18f, 10.4f, 16.6f, 12.6f, 14.5f, 13.7f)
                verticalLineTo(15f)
                close()
            }
        }
    }

    val CheckCircle: ImageVector by lazy {
        strokeIcon("CheckCircle") {
            strokePath(strokeWidth = 2.1f) {
                moveTo(21f, 12f)
                arcTo(9f, 9f, 0f, true, true, 3f, 12f)
                arcTo(9f, 9f, 0f, true, true, 21f, 12f)
                moveTo(8.5f, 12.5f)
                lineTo(10.8f, 14.8f)
                lineTo(15.5f, 9.8f)
            }
        }
    }

    val InfoCircle: ImageVector by lazy {
        strokeIcon("InfoCircle") {
            strokePath(strokeWidth = 2.1f) {
                moveTo(21f, 12f)
                arcTo(9f, 9f, 0f, true, true, 3f, 12f)
                arcTo(9f, 9f, 0f, true, true, 21f, 12f)
                moveTo(12f, 8f)
                verticalLineTo(12.5f)
                moveTo(12f, 16f)
                verticalLineTo(16.2f)
            }
        }
    }

    val Route: ImageVector by lazy {
        strokeIcon("Route") {
            strokePath {
                moveTo(8f, 17f)
                arcTo(2f, 2f, 0f, true, true, 4f, 17f)
                arcTo(2f, 2f, 0f, true, true, 8f, 17f)
                moveTo(20f, 7f)
                arcTo(2f, 2f, 0f, true, true, 16f, 7f)
                arcTo(2f, 2f, 0f, true, true, 20f, 7f)
                moveTo(6f, 15f)
                verticalLineTo(9f)
                curveTo(6f, 7.9f, 6.9f, 7f, 8f, 7f)
                horizontalLineTo(16f)
            }
        }
    }

    val Trophy: ImageVector by lazy {
        strokeIcon("Trophy") {
            strokePath(strokeWidth = 1.9f) {
                moveTo(8f, 21f)
                horizontalLineTo(16f)
                moveTo(12f, 17f)
                verticalLineTo(21f)
                moveTo(7f, 5f)
                horizontalLineTo(17f)
                verticalLineTo(10f)
                curveTo(17f, 12.76f, 14.76f, 15f, 12f, 15f)
                curveTo(9.24f, 15f, 7f, 12.76f, 7f, 10f)
                close()
                moveTo(17f, 6f)
                horizontalLineTo(20f)
                verticalLineTo(8f)
                curveTo(20f, 9.66f, 18.66f, 11f, 17f, 11f)
                moveTo(7f, 6f)
                horizontalLineTo(4f)
                verticalLineTo(8f)
                curveTo(4f, 9.66f, 5.34f, 11f, 7f, 11f)
            }
        }
    }

    val Chart: ImageVector by lazy {
        strokeIcon("Chart") {
            strokePath(strokeWidth = 1.9f) {
                moveTo(3f, 3f)
                verticalLineTo(21f)
                horizontalLineTo(21f)
                moveTo(8f, 16f)
                verticalLineTo(11f)
                moveTo(12f, 16f)
                verticalLineTo(7f)
                moveTo(16f, 16f)
                verticalLineTo(13f)
            }
        }
    }

    val Download: ImageVector by lazy {
        strokeIcon("Download") {
            strokePath {
                moveTo(3f, 16.5f)
                verticalLineTo(18.75f)
                curveTo(3f, 19.99f, 4.01f, 21f, 5.25f, 21f)
                horizontalLineTo(18.75f)
                curveTo(19.99f, 21f, 21f, 19.99f, 21f, 18.75f)
                verticalLineTo(16.5f)
                moveTo(16.5f, 12f)
                lineTo(12f, 16.5f)
                lineTo(7.5f, 12f)
                moveTo(12f, 16.5f)
                verticalLineTo(3f)
            }
        }
    }

    val ArrowRight: ImageVector by lazy {
        strokeIcon("ArrowRight") {
            strokePath(strokeWidth = 2.2f) {
                moveTo(13.5f, 4.5f)
                lineTo(21f, 12f)
                lineTo(13.5f, 19.5f)
                moveTo(21f, 12f)
                horizontalLineTo(3f)
            }
        }
    }

    val ArrowDown: ImageVector by lazy {
        strokeIcon("ArrowDown") {
            strokePath {
                moveTo(12f, 4.5f)
                verticalLineTo(19.5f)
                moveTo(18f, 13.5f)
                lineTo(12f, 19.5f)
                lineTo(6f, 13.5f)
            }
        }
    }

    val Backspace: ImageVector by lazy {
        strokeIcon("Backspace") {
            strokePath(strokeWidth = 2.0f) {
                moveTo(9f, 5f)
                horizontalLineTo(20f)
                curveTo(20.83f, 5f, 21.5f, 5.67f, 21.5f, 6.5f)
                verticalLineTo(17.5f)
                curveTo(21.5f, 18.33f, 20.83f, 19f, 20f, 19f)
                horizontalLineTo(9f)
                lineTo(2.5f, 12f)
                close()
                moveTo(12.5f, 9.5f)
                lineTo(17.5f, 14.5f)
                moveTo(17.5f, 9.5f)
                lineTo(12.5f, 14.5f)
            }
        }
    }
}
