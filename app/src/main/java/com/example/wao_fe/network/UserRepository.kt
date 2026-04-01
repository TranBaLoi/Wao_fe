package com.example.wao_fe.network

import com.example.wao_fe.network.models.RegisterUserRequest
import com.example.wao_fe.network.models.UserResponse
import com.example.wao_fe.network.models.GoogleLoginRequest
import com.example.wao_fe.network.models.VerifyEmailRequest
import com.example.wao_fe.network.models.VerifyEmailResponse
import com.example.wao_fe.network.models.HealthProfileResponse
import com.example.wao_fe.network.models.CreateHealthProfileRequest
import com.example.wao_fe.network.models.DailySummaryResponse
import com.example.wao_fe.network.models.MealPlanResponse
import com.example.wao_fe.network.models.WorkoutProgramRequest
import com.example.wao_fe.network.models.WorkoutProgramResponse
import com.example.wao_fe.network.models.ApplyMealPlanRequest
import com.example.wao_fe.network.models.MealPlanRequest
import com.example.wao_fe.network.models.UpdateUserRequest
import com.example.wao_fe.namstats.models.CreateWeightLogRequest
import com.example.wao_fe.namstats.models.LatestWeightInfoResponse
import com.example.wao_fe.namstats.models.WeightLogUpdateResponse
import com.example.wao_fe.namstats.models.WeightSeriesResponse

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(
        val message: String,
        val status: Int? = null,
        val fieldErrors: Map<String, String> = emptyMap()
    ) : ApiResult<Nothing>()
}

class UserRepository(
    private val apiService: ApiService = NetworkClient.apiService
) {

    suspend fun register(request: RegisterUserRequest): ApiResult<UserResponse> {
        return safeApiCall { apiService.registerUser(request) }
    }

    suspend fun googleLogin(idToken: String): ApiResult<UserResponse> {
        return safeApiCall {
            apiService.loginWithGoogle(GoogleLoginRequest(idToken))
        }
    }

    suspend fun verifyEmail(email: String, code: String): ApiResult<VerifyEmailResponse> {
        return safeApiCall {
            apiService.verifyEmail(VerifyEmailRequest(email, code))
        }
    }

    suspend fun loginByEmail(email: String): ApiResult<UserResponse> {
        return safeApiCall {
            val users = apiService.getUsers()
            users.firstOrNull { it.email.equals(email, ignoreCase = true) }
                ?: throw IllegalArgumentException("Không tìm thấy tài khoản với email này")
        }
    }

    suspend fun getUserById(userId: Long): ApiResult<UserResponse> {
        return safeApiCall {
            apiService.getUserById(userId)
        }
    }

    suspend fun updateUser(userId: Long, request: UpdateUserRequest): ApiResult<UserResponse> {
        return safeApiCall {
            apiService.updateUser(userId, request)
        }
    }

    suspend fun getLatestHealthProfile(userId: Long): ApiResult<HealthProfileResponse> {
        return safeApiCall {
            apiService.getLatestHealthProfile(userId)
        }
    }

    suspend fun getHealthProfileHistory(userId: Long): ApiResult<List<HealthProfileResponse>> {
        return safeApiCall {
            apiService.getHealthProfileHistory(userId)
        }
    }

    suspend fun createHealthProfile(userId: Long, request: CreateHealthProfileRequest): ApiResult<HealthProfileResponse> {
        return safeApiCall {
            apiService.createHealthProfile(userId, request)
        }
    }

    suspend fun getTodaySummary(userId: Long): ApiResult<DailySummaryResponse> {
        return safeApiCall {
            apiService.getTodaySummary(userId)
        }
    }

    suspend fun getMealPlanById(id: Long): ApiResult<MealPlanResponse> {
        return safeApiCall { apiService.getMealPlanById(id) }
    }

    suspend fun getUserMealPlans(userId: Long): ApiResult<List<MealPlanResponse>> {
        return safeApiCall { apiService.getUserMealPlans(userId) }
    }

    suspend fun createMealPlan(request: MealPlanRequest): ApiResult<MealPlanResponse> {
        return safeApiCall { apiService.createMealPlan(request) }
    }

    suspend fun updateMealPlan(mealPlanId: Long, request: MealPlanRequest): ApiResult<MealPlanResponse> {
        return safeApiCall { apiService.updateMealPlan(mealPlanId, request) }
    }

    suspend fun generateMealPlan(userId: Long, date: String): ApiResult<MealPlanResponse> {
        return safeApiCall { apiService.generateMealPlan(userId, date) }
    }

    suspend fun applyMealPlan(mealPlanId: Long, request: ApplyMealPlanRequest): ApiResult<Unit> {
        return safeApiCall { apiService.applyMealPlan(mealPlanId, request) }
    }

    suspend fun deleteMealPlan(mealPlanId: Long): ApiResult<Unit> {
        return safeApiCall { apiService.deleteMealPlan(mealPlanId) }
    }

    // Workout programs
    suspend fun createWorkoutProgram(request: WorkoutProgramRequest): ApiResult<WorkoutProgramResponse> {
        return safeApiCall { apiService.createWorkoutProgram(request) }
    }

    suspend fun getWeightSeries(
        userId: Long,
        from: String,
        to: String,
        groupBy: String
    ): ApiResult<WeightSeriesResponse> {
        return safeApiCall { apiService.getWeightSeries(userId, from, to, groupBy) }
    }

    suspend fun getLatestWeightInfo(userId: Long): ApiResult<LatestWeightInfoResponse> {
        return safeApiCall { apiService.getLatestWeightInfo(userId) }
    }

    suspend fun createWeightLog(userId: Long, request: CreateWeightLogRequest): ApiResult<WeightLogUpdateResponse> {
        return safeApiCall { apiService.createWeightLog(userId, request) }
    }

    private suspend fun <T> safeApiCall(call: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(call())
        } catch (exception: IllegalArgumentException) {
            ApiResult.Error(message = exception.message ?: "Dữ liệu không hợp lệ")
        } catch (exception: Exception) {
            ApiResult.Error(message = exception.message ?: "Không thể kết nối máy chủ")
        }
    }
}
