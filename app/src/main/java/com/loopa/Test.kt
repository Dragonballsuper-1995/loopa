package com.loopa.app

import androidx.compose.material3.*
import androidx.compose.runtime.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSlider() {
    RangeSlider(
        value = 0f..1f,
        onValueChange = {},
        startThumb = { },
        endThumb = { },
        track = { }
    )
}
