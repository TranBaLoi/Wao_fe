package com.example.wao_fe.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.UserRepository
import com.example.wao_fe.network.models.ApplyMealPlanRequest
import com.example.wao_fe.network.models.MealPlanFoodRequest
import com.example.wao_fe.network.models.MealPlanResponse
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class MealPlanState {
    object Idle : MealPlanState()
    object Loading : MealPlanState()
    data class Success(val data: MealPlanResponse) : MealPlanState()
    data class Error(val message: String) : MealPlanState()
    object Applying : MealPlanState()
    object AppliedSuccess : MealPlanState()
    data class AppliedError(val message: String) : MealPlanState()
}

class MealPlanViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _state = MutableLiveData<MealPlanState>(MealPlanState.Idle)
    val state: LiveData<MealPlanState> = _state

    private var currentMealPlan: MealPlanResponse? = null

    fun generateMealPlan(userId: Long) {
        _state.value = MealPlanState.Loading
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            when (val result = userRepository.generateMealPlan(userId, dateStr)) {
                is ApiResult.Success -> {
                    currentMealPlan = result.data
                    _state.value = MealPlanState.Success(result.data)
                }
                is ApiResult.Error -> {
                    _state.value = MealPlanState.Error(result.message)
                }
            }
        }
    }

    fun applyMealPlan(userId: Long) {
        val plan = currentMealPlan ?: return
        val planId = plan.id ?: 0L
        _state.value = MealPlanState.Applying
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val transientFoods = plan.foods.map { food ->
                MealPlanFoodRequest(
                    foodId = food.foodId,
                    mealType = food.mealType,
                    servingQty = food.servingQty
                )
            }

            val request = ApplyMealPlanRequest(userId, dateStr, transientFoods)
            when (val result = userRepository.applyMealPlan(planId, request)) {
                is ApiResult.Success -> {
                    _state.value = MealPlanState.AppliedSuccess
                }
                is ApiResult.Error -> {
                    _state.value = MealPlanState.AppliedError(result.message)
                }
            }
        }
    }
}
