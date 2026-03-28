package com.example.wao_fe.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.UserRepository
import com.example.wao_fe.network.models.HealthProfileResponse
import com.example.wao_fe.network.models.UserResponse
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _user = MutableLiveData<UserResponse?>()
    val user: LiveData<UserResponse?> = _user

    private val _healthProfile = MutableLiveData<HealthProfileResponse?>()
    val healthProfile: LiveData<HealthProfileResponse?> = _healthProfile

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    fun loadData(userId: Long) {
        if (userId == -1L) return

        viewModelScope.launch {
            // Load User Info
            when (val res = userRepository.getUserById(userId)) {
                is ApiResult.Success -> {
                    _user.value = res.data
                }
                is ApiResult.Error -> {
                    _error.value = "Lỗi tải thông tin user: ${res.message}"
                }
            }

            // Load Health Profile
            when (val hpRes = userRepository.getLatestHealthProfile(userId)) {
                is ApiResult.Success -> {
                    _healthProfile.value = hpRes.data
                }
                is ApiResult.Error -> {
                    // It's okay if not found, maybe they haven't set it up
                    // _error.value = "Lỗi tải health profile: ${hpRes.message}"
                }
            }
        }
    }
}
