package com.efedorchenko.timely.data

import kotlin.time.Duration

data class SummaryData(
    val daysCount: Int,
    val warkDuration: Duration,
    val fines: HashSet<Fine>
)
