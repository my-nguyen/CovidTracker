package com.nguyen.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.ticker.TickerUtils
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "MainActivity"
private const val BASE_URL = "https://api.covidtracking.com/v1/"
private const val ALL_STATES = "All (Nationwide)"

class MainActivity : AppCompatActivity() {
    private lateinit var currentlyShownData: List<Data>
    private lateinit var adapter: CovidSparkAdapter
    private lateinit var perStateDailyData: Map<String, List<Data>>
    private lateinit var nationalDailyData: List<Data>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = getString(R.string.app_description)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss").create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        val service = retrofit.create(CovidService::class.java)
        service.getNationalData().enqueue(object: Callback<List<Data>> {
            override fun onResponse(call: Call<List<Data>>, response: Response<List<Data>>) {
                Log.i(TAG, "onResponse $response")
                val body = response.body()
                if (body == null) {
                    Log.w(TAG, "Did not receive a valid response body")
                } else {
                    setupEventListeners()
                    // call reversed() to start with oldest data first, for graphing purpose
                    nationalDailyData = body.reversed()
                    Log.i(TAG, "Update graph with national data")
                    updateDisplayWithData(nationalDailyData)
                }
            }

            override fun onFailure(call: Call<List<Data>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }
        })

        service.getStatesData().enqueue(object: Callback<List<Data>> {
            override fun onResponse(call: Call<List<Data>>, response: Response<List<Data>>) {
                Log.i(TAG, "onResponse $response")
                val body = response.body()
                if (body == null) {
                    Log.w(TAG, "Did not receive a valid response body")
                } else {
                    // body.reversed() to start with oldest data first, for graphing purpose
                    perStateDailyData = body.reversed().groupBy { it.state }
                    Log.i(TAG, "Update spinner with state names")
                    updateSpinnerWithStateData(perStateDailyData.keys)
                }
            }

            override fun onFailure(call: Call<List<Data>>, t: Throwable) {
                Log.e(TAG, "onFailure $t")
            }
        })
    }

    private fun updateSpinnerWithStateData(states: Set<String>) {
        val abbreviations = states.toMutableList()
        abbreviations.sort()
        // add the overall national data
        abbreviations.add(0, ALL_STATES)

        // add state abbreviations as data source for the spinner
        spinner_state.attachDataSource(abbreviations)
        spinner_state.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
            val state = parent.getItemAtPosition(position) as String
            val data = perStateDailyData[state] ?: nationalDailyData
            updateDisplayWithData(data)
        }
    }

    private fun setupEventListeners() {
        ticker_metric.setCharacterLists(TickerUtils.provideNumberList())

        // add a listener for the user scrubbing on the chart
        spark_view.isScrubEnabled = true
        spark_view.setScrubListener {
            if (it is Data) {
                updateInfoForDate(it)
            }
        }
        // respond to radio button selected events
        radio_group_time.setOnCheckedChangeListener { _, checkedId ->
            adapter.daysAgo = when (checkedId) {
                R.id.radio_week -> TimeScale.WEEK
                R.id.radio_month -> TimeScale.MONTH
                else -> TimeScale.MAX
            }
            adapter.notifyDataSetChanged()
        }
        radio_group_metric.setOnCheckedChangeListener{ _, checkedId ->
            when (checkedId) {
                R.id.radio_negative -> updateDisplayMetric(Metric.NEGATIVE)
                R.id.radio_positive -> updateDisplayMetric(Metric.POSITIVE)
                R.id.radio_death -> updateDisplayMetric(Metric.DEATH)
            }
        }
    }

    private fun updateDisplayMetric(metric: Metric) {
        // update the color of the chart
        val colorRes = when (metric) {
            Metric.NEGATIVE -> R.color.negative
            Metric.POSITIVE -> R.color.positive
            Metric.DEATH -> R.color.death
        }
        @ColorInt val color = ContextCompat.getColor(this, colorRes)
        spark_view.lineColor = color
        ticker_metric.setTextColor(color)

        // update the metric on the adapter
        adapter.metric = metric
        adapter.notifyDataSetChanged()

        // reset number and date shown in the bottom text views
        updateInfoForDate(currentlyShownData.last())
    }

    private fun updateDisplayWithData(dailyData: List<Data>) {
        currentlyShownData = dailyData

        // create a SparkAdapter with data
        adapter = CovidSparkAdapter(dailyData)
        spark_view.adapter = adapter

        // update radio buttons to select the positive cases and max time by default
        radio_positive.isChecked = true
        radio_max.isChecked = true

        // display metric for the most recent date
        updateDisplayMetric(Metric.POSITIVE)
    }

    private fun updateInfoForDate(data: Data) {
        val numCases = when (adapter.metric) {
            Metric.NEGATIVE -> data.negativeIncrease
            Metric.POSITIVE -> data.positiveIncrease
            Metric.DEATH -> data.deathIncrease
        }
        val metricLabel = NumberFormat.getInstance().format(numCases)
        ticker_metric.text = metricLabel
        val dateLabel = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(data.dateChecked)
        label_date.text = dateLabel
    }
}