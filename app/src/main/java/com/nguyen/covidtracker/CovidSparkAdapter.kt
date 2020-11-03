package com.nguyen.covidtracker

import android.graphics.RectF
import com.robinhood.spark.SparkAdapter

class CovidSparkAdapter(private val dailyData: List<Data>) : SparkAdapter() {

    var metric = Metric.POSITIVE
    var daysAgo = TimeScale.MAX

    override fun getCount() = dailyData.size

    override fun getItem(index: Int) = dailyData[index]

    override fun getY(index: Int): Float {
        val data = dailyData[index]
        return when (metric) {
            Metric.NEGATIVE -> data.negativeIncrease.toFloat()
            Metric.POSITIVE -> data.positiveIncrease.toFloat()
            Metric.DEATH -> data.deathIncrease.toFloat()
        }
    }

    override fun getDataBounds(): RectF {
        val bounds = super.getDataBounds()
        if (daysAgo != TimeScale.MAX) {
            // show only the last one week's or one month's worth of data
            bounds.left = count - daysAgo.numDays.toFloat()
        }
        return bounds
    }
}
