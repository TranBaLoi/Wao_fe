package com.example.wao_fe.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

// OpenFoodFacts Product Response Models
data class ProductResponse(
    val code: String,
    val product: ProductData?,
    val status: Int,
    val status_verbose: String
)

data class ProductData(
    val product_name: String?,
    val generic_name: String?,
    val image_url: String?,
    val image_front_url: String?,
    val image_nutrition_url: String?,
    val ingredients_text: String?,
    val nutriments: Nutriments?
)

data class Nutriments(
    val energy: Double?,
    val energy_100g: Double?,
    val energy_kcal: Double?,
    val energy_kcal_100g: Double?,
    val carbohydrates_100g: Double?,
    val proteins_100g: Double?,
    val fat_100g: Double?
)

interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProductInfo(@Path("barcode") barcode: String): ProductResponse

    companion object {
        private const val BASE_URL = "https://world.openfoodfacts.org/"

        fun create(): OpenFoodFactsApi {
            val retrofit = Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
            return retrofit.create(OpenFoodFactsApi::class.java)
        }
    }
}
