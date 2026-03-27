package com.example.wao_fe.network

import com.example.wao_fe.network.models.RegisterUserRequest
import com.example.wao_fe.network.models.UserResponse
import com.example.wao_fe.network.models.GoogleLoginRequest
import com.example.wao_fe.network.models.VerifyEmailRequest
import com.example.wao_fe.network.models.VerifyEmailResponse
import com.example.wao_fe.network.models.HealthProfileResponse
import com.example.wao_fe.network.models.CreateHealthProfileRequest
import com.example.wao_fe.network.models.DailySummaryResponse

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

    suspend fun getLatestHealthProfile(userId: Long): ApiResult<HealthProfileResponse> {
        return safeApiCall {
            apiService.getLatestHealthProfile(userId)
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
