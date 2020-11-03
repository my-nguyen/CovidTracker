package com.nguyen.covidtracker

import java.util.*

data class Data(
    val dateChecked: Date,
    val deathIncrease: Int,
    val negativeIncrease: Int,
    val positiveIncrease: Int,
    val state: String
)