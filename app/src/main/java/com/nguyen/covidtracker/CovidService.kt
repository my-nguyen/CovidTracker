package com.nguyen.covidtracker

import retrofit2.Call
import retrofit2.http.GET

interface CovidService {
    @GET("us/daily.json")
    fun getNationalData() : Call<List<Data>>

    @GET("states/daily.json")
    fun getStatesData() : Call<List<Data>>
}