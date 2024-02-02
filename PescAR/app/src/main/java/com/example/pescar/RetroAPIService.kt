package com.example.pescar

import com.google.gson.JsonObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val BASE_URL =
    "https://pawgab.pythonanywhere.com"
    //"http://10.0.2.2:5000"

private val retrofit = Retrofit.Builder()
    .addConverterFactory(GsonConverterFactory.create())
    .baseUrl(BASE_URL)
    .build()

object RetroAPI{
    val retrofitService : RetroAPIService by lazy {
        retrofit.create(RetroAPIService::class.java)
    }
}

interface RetroAPIService {
    @GET("/")
    suspend fun getFishInfo(@Query("id") id: Int): JsonObject

    @GET("/fishcount")
    suspend fun getFishCount(): String
}