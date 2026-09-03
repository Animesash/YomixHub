package com.yomixhub.android.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.ui.unit.LayoutDirection
import kotlin.comparisons.maxOf

/**
 * Element-wise max merge of two [PaddingValues] – combines window insets
 * (from `Scaffold.innerPadding`) with the design margins of a screen so a
 * lazy list can use them as one `contentPadding`.
 */
fun PaddingValues.mergedWith(other: PaddingValues): PaddingValues {
    val ltr = LayoutDirection.Ltr
    return PaddingValues(
        start = maxOf(calculateStartPadding(ltr), other.calculateStartPadding(ltr)),
        top = maxOf(calculateTopPadding(), other.calculateTopPadding()),
        end = maxOf(calculateEndPadding(ltr), other.calculateEndPadding(ltr)),
        bottom = maxOf(calculateBottomPadding(), other.calculateBottomPadding()),
    )
}
