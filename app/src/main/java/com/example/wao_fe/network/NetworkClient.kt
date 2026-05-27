package com.example.wao_fe.network

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Đối tượng cấu hình Retrofit dùng chung cho toàn bộ ứng dụng.
 * Chứa BASE_URL backend, OkHttpClient, Gson converter và instance ApiService.
 */
object NetworkClient {

    // Khi chạy trên Android Emulator, 10.0.2.2 được dùng để truy cập localhost của máy tính chạy backend.
    private const val BASE_URL = "http://10.0.2.2:8080/"
//    172.20.10.2

    val baseUrl: String
        get() = BASE_URL

    val gson: Gson = GsonBuilder().create()

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    val apiService: ApiService = retrofit.create(ApiService::class.java)
}
