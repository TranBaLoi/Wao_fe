package com.example.wao_fe.network

import com.example.wao_fe.namstats.models.DailyNutritionResponse
import com.example.wao_fe.namstats.models.NutritionSeriesResponse
import com.example.wao_fe.namstats.models.WeightSeriesResponse
import com.example.wao_fe.network.models.CreateFoodLogRequest
import com.example.wao_fe.network.models.CreateHealthProfileRequest
import com.example.wao_fe.network.models.CreateStepLogRequest
import com.example.wao_fe.network.models.CreateWaterLogRequest
import com.example.wao_fe.network.models.CreateWorkoutLogRequest
import com.example.wao_fe.network.models.DailySummaryResponse
import com.example.wao_fe.network.models.ExerciseRequest
import com.example.wao_fe.network.models.ExerciseResponse
import com.example.wao_fe.network.models.FoodLogResponse
import com.example.wao_fe.network.models.FoodRequest
import com.example.wao_fe.network.models.FoodResponse
import com.example.wao_fe.network.models.HealthProfileResponse
import com.example.wao_fe.network.models.MealPlanRequest
import com.example.wao_fe.network.models.MealPlanResponse
import com.example.wao_fe.network.models.ProgramLevel
import com.example.wao_fe.network.models.RegisterUserRequest
import com.example.wao_fe.network.models.StepLogResponse
import com.example.wao_fe.network.models.UpdateUserRequest
import com.example.wao_fe.network.models.UserResponse
import com.example.wao_fe.network.models.WaterLogResponse
import com.example.wao_fe.network.models.WorkoutLogResponse
import com.example.wao_fe.network.models.WorkoutProgramRequest
import com.example.wao_fe.network.models.WorkoutProgramResponse
import com.example.wao_fe.network.models.GoogleLoginRequest
import com.example.wao_fe.network.models.VerifyEmailRequest
import com.example.wao_fe.network.models.VerifyEmailResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    // Users
    @POST("api/users/register")
    suspend fun registerUser(@Body request: RegisterUserRequest): UserResponse

    @POST("api/users/google-login")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): UserResponse

    @POST("api/users/verify")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): VerifyEmailResponse

    @GET("api/users")
    suspend fun getUsers(): List<UserResponse>

    @GET("api/users/{id}")
    suspend fun getUserById(@Path("id") id: Long): UserResponse

    @PUT("api/users/{id}")
    suspend fun updateUser(
        @Path("id") id: Long,
        @Body request: UpdateUserRequest
    ): UserResponse

    @DELETE("api/users/{id}")
    suspend fun deleteUser(@Path("id") id: Long)

    // Health profiles
    @POST("api/users/{userId}/health-profiles")
    suspend fun createHealthProfile(
        @Path("userId") userId: Long,
        @Body request: CreateHealthProfileRequest
    ): HealthProfileResponse

    @GET("api/users/{userId}/health-profiles/latest")
    suspend fun getLatestHealthProfile(@Path("userId") userId: Long): HealthProfileResponse

    @GET("api/users/{userId}/health-profiles/history")
    suspend fun getHealthProfileHistory(@Path("userId") userId: Long): List<HealthProfileResponse>

    // Foods
    @POST("api/foods")
    suspend fun createFood(@Body request: FoodRequest): FoodResponse

    @POST("api/foods/admin")
    suspend fun createAdminFood(@Body request: FoodRequest): FoodResponse

    @GET("api/foods")
    suspend fun getFoods(@Query("name") name: String? = null): List<FoodResponse>

    @GET("api/foods/{id}")
    suspend fun getFoodById(@Path("id") id: Long): FoodResponse

    @PUT("api/foods/{id}")
    suspend fun updateFood(
        @Path("id") id: Long,
        @Body request: FoodRequest
    ): FoodResponse

    @DELETE("api/foods/{id}")
    suspend fun deleteFood(@Path("id") id: Long)

    // Food logs
    @POST("api/users/{userId}/food-logs")
    suspend fun createFoodLog(
        @Path("userId") userId: Long,
        @Body request: CreateFoodLogRequest
    ): FoodLogResponse

    @GET("api/users/{userId}/food-logs")
    suspend fun getFoodLogs(
        @Path("userId") userId: Long,
        @Query("date") date: String
    ): List<FoodLogResponse>

    @DELETE("api/users/{userId}/food-logs/{logId}")
    suspend fun deleteFoodLog(
        @Path("userId") userId: Long,
        @Path("logId") logId: Long
    )

    // Exercises
    @POST("api/exercises")
    suspend fun createExercise(@Body request: ExerciseRequest): ExerciseResponse

    @GET("api/exercises")
    suspend fun getExercises(@Query("name") name: String? = null): List<ExerciseResponse>

    @GET("api/exercises/category/{categoryId}")
    suspend fun getExercisesByCategory(@Path("categoryId") categoryId: Long): List<ExerciseResponse>

    @GET("api/exercises/{id}")
    suspend fun getExerciseById(@Path("id") id: Long): ExerciseResponse

    @DELETE("api/exercises/{id}")
    suspend fun deleteExercise(@Path("id") id: Long)

    // Workout programs
    @POST("api/workout-programs")
    suspend fun createWorkoutProgram(@Body request: WorkoutProgramRequest): WorkoutProgramResponse

    @GET("api/workout-programs")
    suspend fun getWorkoutPrograms(@Query("level") level: ProgramLevel? = null): List<WorkoutProgramResponse>

    @GET("api/workout-programs/{id}")
    suspend fun getWorkoutProgramById(@Path("id") id: Long): WorkoutProgramResponse

    @DELETE("api/workout-programs/{id}")
    suspend fun deleteWorkoutProgram(@Path("id") id: Long)

    // Meal plans
    @POST("api/meal-plans")
    suspend fun createMealPlan(@Body request: MealPlanRequest): MealPlanResponse

    @GET("api/meal-plans")
    suspend fun getMealPlans(): List<MealPlanResponse>

    @GET("api/meal-plans/system")
    suspend fun getSystemMealPlans(): List<MealPlanResponse>

    @GET("api/meal-plans/user/{userId}")
    suspend fun getUserMealPlans(@Path("userId") userId: Long): List<MealPlanResponse>

    @GET("api/meal-plans/{id}")
    suspend fun getMealPlanById(@Path("id") id: Long): MealPlanResponse

    @DELETE("api/meal-plans/{id}")
    suspend fun deleteMealPlan(@Path("id") id: Long)

    // Workout logs
    @POST("api/users/{userId}/workout-logs")
    suspend fun createWorkoutLog(
        @Path("userId") userId: Long,
        @Body request: CreateWorkoutLogRequest
    ): WorkoutLogResponse

    @GET("api/users/{userId}/workout-logs")
    suspend fun getWorkoutLogs(
        @Path("userId") userId: Long,
        @Query("date") date: String
    ): List<WorkoutLogResponse>

    @DELETE("api/users/{userId}/workout-logs/{logId}")
    suspend fun deleteWorkoutLog(
        @Path("userId") userId: Long,
        @Path("logId") logId: Long
    )

    // Step logs
    @POST("api/users/{userId}/step-logs")
    suspend fun upsertStepLog(
        @Path("userId") userId: Long,
        @Body request: CreateStepLogRequest
    ): StepLogResponse

    @GET("api/users/{userId}/step-logs/date")
    suspend fun getStepLogByDate(
        @Path("userId") userId: Long,
        @Query("date") date: String
    ): StepLogResponse

    @GET("api/users/{userId}/step-logs")
    suspend fun getStepLogsByRange(
        @Path("userId") userId: Long,
        @Query("from") from: String,
        @Query("to") to: String
    ): List<StepLogResponse>

    // Water logs
    @POST("api/users/{userId}/water-logs")
    suspend fun createWaterLog(
        @Path("userId") userId: Long,
        @Body request: CreateWaterLogRequest
    ): WaterLogResponse

    @GET("api/users/{userId}/water-logs")
    suspend fun getWaterLogs(
        @Path("userId") userId: Long,
        @Query("date") date: String
    ): List<WaterLogResponse>

    @GET("api/users/{userId}/water-logs/total")
    suspend fun getWaterTotal(
        @Path("userId") userId: Long,
        @Query("date") date: String
    ): Int

    @DELETE("api/users/{userId}/water-logs/{logId}")
    suspend fun deleteWaterLog(
        @Path("userId") userId: Long,
        @Path("logId") logId: Long
    )

    // Daily summaries
    @GET("api/users/{userId}/daily-summaries/today")
    suspend fun getTodaySummary(@Path("userId") userId: Long): DailySummaryResponse

    @GET("api/users/{userId}/daily-summaries")
    suspend fun getSummaryByDate(
        @Path("userId") userId: Long,
        @Query("date") date: String
    ): DailySummaryResponse

    @GET("api/users/{userId}/daily-summaries/history")
    suspend fun getSummaryHistory(
        @Path("userId") userId: Long,
        @Query("from") from: String,
        @Query("to") to: String
    ): List<DailySummaryResponse>

    @POST("api/users/{userId}/daily-summaries/refresh")
    suspend fun refreshSummary(
        @Path("userId") userId: Long,
        @Query("date") date: String? = null
    ): DailySummaryResponse

    //nam them
    @GET("api/users/{userId}/statistics/nutrition/daily")
    suspend fun getDailyNutrition(
        @Path("userId") userId: Long,
        @Query("date") date: String
    ): DailyNutritionResponse

    @GET("api/users/{userId}/statistics/nutrition")
    suspend fun getNutritionSeries(
        @Path("userId") userId: Long,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("groupBy") groupBy: String
    ): NutritionSeriesResponse

    @GET("api/users/{userId}/statistics/weight")
    suspend fun getWeightSeries(
        @Path("userId") userId: Long,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("groupBy") groupBy: String
    ): WeightSeriesResponse
}
