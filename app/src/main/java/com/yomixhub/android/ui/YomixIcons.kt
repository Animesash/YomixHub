package com.yomixhub.android.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * YomixHub icon set.
 *
 * Material Symbols geometry expressed as [ImageVector]s. The classic
 * `androidx.compose.material:material-icons-*` artifacts are deprecated and no
 * longer ship with the Compose BOM, so the app owns a minimal, tree-shakeable
 * set of vector icons instead (24dp grid, matching the Material Symbols specs).
 */
object YomixIcons {

    /** Top app bar – search. */
    val Search: ImageVector by lazy {
        icon("Search") {
            addPath(
                pathData = addPathNodes(
                    "M15.5 14h-.79l-.28-.27C15.41 12.59 16 11.11 16 9.5 16 5.91 13.09 3 9.5 3S3 5.91 3 9.5 " +
                        "5.91 16 9.5 16c1.61 0 3.09-.59 4.23-1.57l.27.28v.79l5 4.99L20.49 19l-4.99-5z" +
                        "m-6 0C7.01 14 5 11.99 5 9.5S7.01 5 9.5 5 14 7.01 14 9.5 11.99 14 9.5 14z",
                ),
                fill = SolidColor(Color.Black),
            )
        }
    }

    /** Top app bar – close / dismiss search. */
    val Close: ImageVector by lazy {
        icon("Close") {
            addPath(
                pathData = addPathNodes(
                    "M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 " +
                        "17.59 19 19 17.59 13.41 12z",
                ),
                fill = SolidColor(Color.Black),
            )
        }
    }

    /** Details / reader – navigate back. */
    val ArrowBack: ImageVector by lazy {
        icon("ArrowBack") {
            addPath(
                pathData = addPathNodes(
                    "M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z",
                ),
                fill = SolidColor(Color.Black),
            )
        }
    }

    /** Top app bar – settings (gear). */
    val Settings: ImageVector by lazy {
        icon("Settings") {
            addPath(
                pathData = addPathNodes(
                    "M19.14,12.94c0.04-0.3,0.06-0.61,0.06-0.94c0-0.32-0.02-0.64-0.07-0.94l2.03-1.58" +
                        "c0.18-0.14,0.23-0.41,0.12-0.61l-1.92-3.32c-0.12-0.22-0.37-0.29-0.59-0.22" +
                        "l-2.39,0.96c-0.5-0.38-1.03-0.7-1.62-0.94L14.4,2.81c-0.04-0.24-0.24-0.41-0.48-0.41" +
                        "h-3.84c-0.24,0-0.43,0.17-0.47,0.41L9.25,5.35C8.66,5.59,8.12,5.92,7.63,6.29" +
                        "L5.24,5.33c-0.22-0.08-0.47,0-0.59,0.22L2.74,8.87C2.62,9.08,2.66,9.34,2.86,9.48" +
                        "l2.03,1.58C4.84,11.36,4.8,11.69,4.8,12s0.02,0.64,0.07,0.94l-2.03,1.58" +
                        "c-0.18,0.14-0.23,0.41-0.12,0.61l1.92,3.32c0.12,0.22,0.37,0.29,0.59,0.22" +
                        "l2.39-0.96c0.5,0.38,1.03,0.7,1.62,0.94l0.36,2.54c0.05,0.24,0.24,0.41,0.48,0.41" +
                        "h3.84c0.24,0,0.44-0.17,0.47-0.41l0.36-2.54c0.59-0.24,1.13-0.56,1.62-0.94" +
                        "l2.39,0.96c0.22,0.08,0.47,0,0.59-0.22l1.92-3.32c0.12-0.22,0.07-0.47-0.12-0.61" +
                        "L19.14,12.94z M12,15.6c-1.98,0-3.6-1.62-3.6-3.6s1.62-3.6,3.6-3.6s3.6,1.62,3.6,3.6" +
                        "S13.98,15.6,12,15.6z",
                ),
                fill = SolidColor(Color.Black),
            )
        }
    }

    // ------------------------------------------------------------------ //
    // Bottom navigation
    // ------------------------------------------------------------------ //

    /** "Закладки" – filled when the tab is active. */
    val BookmarkFilled: ImageVector by lazy {
        icon("BookmarkFilled") {
            addPath(
                pathData = addPathNodes(
                    "M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z",
                ),
                fill = SolidColor(Color.Black),
            )
        }
    }

    /** "Закладки" – outlined when the tab is inactive. */
    val BookmarkOutlined: ImageVector by lazy {
        icon("BookmarkOutlined") {
            addPath(
                pathData = addPathNodes(
                    "M17 3H7c-1.1 0-1.99.9-1.99 2L5 21l7-3 7 3V5c0-1.1-.9-2-2-2z" +
                        "m0 15l-5-2.18L7 18V5h10v13z",
                ),
                fill = SolidColor(Color.Black),
            )
        }
    }

    /** "Все тайтлы" – filled catalog grid. */
    val GridFilled: ImageVector by lazy {
        icon("GridFilled") {
            roundedSquare(3f, 3f)
            roundedSquare(13f, 3f)
            roundedSquare(3f, 13f)
            roundedSquare(13f, 13f)
        }
    }

    /** "Все тайтлы" – outlined catalog grid. */
    val GridOutlined: ImageVector by lazy {
        icon("GridOutlined") {
            addPath(
                pathData = roundedSquareNodes(3f, 3f, 8f, 8f, 2f),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            addPath(
                pathData = roundedSquareNodes(13f, 3f, 8f, 8f, 2f),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            addPath(
                pathData = roundedSquareNodes(3f, 13f, 8f, 8f, 2f),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            addPath(
                pathData = roundedSquareNodes(13f, 13f, 8f, 8f, 2f),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }

    /** "Уведомления" – filled bell. */
    val NotificationsFilled: ImageVector by lazy {
        icon("NotificationsFilled") {
            addPath(
                pathData = addPathNodes(
                    "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2z" +
                        "m6-6v-5c0-3.07-1.64-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 " +
                        "6 7.92 6 11v5l-2 2v1h16v-1l-2-2z",
                ),
                fill = SolidColor(Color.Black),
            )
        }
    }

    /** "Уведомления" – outlined bell. */
    val NotificationsOutlined: ImageVector by lazy {
        icon("NotificationsOutlined") {
            addPath(
                pathData = addPathNodes(
                    "M12 22c1.1 0 2-.9 2-2h-4c0 1.1.89 2 2 2z" +
                        "m6-6v-5c0-3.07-1.63-5.64-4.5-6.32V4c0-.83-.67-1.5-1.5-1.5s-1.5.67-1.5 1.5v.68C7.63 5.36 " +
                        "6 7.92 6 11v5l-2 2v1h16v-1l-2-2zm-2 1H8v-6c0-2.48 1.51-4.5 4-4.5s4 2.02 4 4.5v6z",
                ),
                fill = SolidColor(Color.Black),
            )
        }
    }

    /** "Загрузки" – filled download. */
    val DownloadFilled: ImageVector by lazy {
        icon("DownloadFilled") {
            addPath(
                pathData = addPathNodes("M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"),
                fill = SolidColor(Color.Black),
            )
        }
    }

    /** "Загрузки" – outlined download. */
    val DownloadOutlined: ImageVector by lazy {
        icon("DownloadOutlined") {
            addPath(
                pathData = listOf(
                    PathNode.MoveTo(12f, 4.5f),
                    PathNode.LineTo(12f, 14.8f),
                    PathNode.MoveTo(7.7f, 10.5f),
                    PathNode.LineTo(12f, 14.8f),
                    PathNode.LineTo(16.3f, 10.5f),
                    PathNode.MoveTo(5f, 18.5f),
                    PathNode.LineTo(19f, 18.5f),
                ),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }

    // ------------------------------------------------------------------ //
    // Misc screen icons
    // ------------------------------------------------------------------ //

    /** Downloads screen – pause. */
    val Pause: ImageVector by lazy {
        icon("Pause") {
            addPath(
                pathData = roundedSquareNodes(7.2f, 5f, 3.6f, 14f, 1.8f),
                fill = SolidColor(Color.Black),
            )
            addPath(
                pathData = roundedSquareNodes(13.2f, 5f, 3.6f, 14f, 1.8f),
                fill = SolidColor(Color.Black),
            )
        }
    }

    /** Downloads screen – completed. */
    val Check: ImageVector by lazy {
        icon("Check") {
            addPath(
                pathData = addPathNodes("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"),
                fill = SolidColor(Color.Black),
            )
        }
    }

    /** Downloads screen – failed. */
    val Error: ImageVector by lazy {
        icon("Error") {
            addPath(
                pathData = addPathNodes("M1 21h22L12 2 1 21zm12-3h-2v-2h2v2zm0-4h-2v-4h2v4z"),
                fill = SolidColor(Color.Black),
            )
        }
    }
}

// ---------------------------------------------------------------------- //
// Builders
// ---------------------------------------------------------------------- //

private const val VIEWPORT = 24f

private fun icon(
    name: String,
    builder: ImageVector.Builder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = VIEWPORT,
    viewportHeight = VIEWPORT,
).apply(builder).build()

/** Adds one of the grid-view rounded squares (8×8, r = 2). */
private fun ImageVector.Builder.roundedSquare(x: Float, y: Float) {
    addPath(
        pathData = roundedSquareNodes(x, y, 8f, 8f, 2f),
        fill = SolidColor(Color.Black),
    )
}

/**
 * Builds an SVG-like rounded rectangle path ("M / H / A / V / Z") and parses it
 * with [addPathNodes].
 *
 * Going through the SVG string keeps this helper independent from the exact
 * [PathNode.ArcTo] constructor signature, which changed across Compose
 * versions (arc endpoints are now `arcStartX/arcStartY/arcEndX/arcEndY`).
 */
private fun roundedSquareNodes(
    x: Float,
    y: Float,
    width: Float,
    height: Float,
    radius: Float,
): List<PathNode> = addPathNodes(
    "M ${x + radius} $y" +
        " H ${x + width - radius}" +
        " A $radius $radius 0 0 1 ${x + width} ${y + radius}" +
        " V ${y + height - radius}" +
        " A $radius $radius 0 0 1 ${x + width - radius} ${y + height}" +
        " H ${x + radius}" +
        " A $radius $radius 0 0 1 $x ${y + height - radius}" +
        " V ${y + radius}" +
        " A $radius $radius 0 0 1 ${x + radius} $y" +
        " Z",
)
