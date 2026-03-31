package com.example.wao_fe.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.UserRepository
import com.example.wao_fe.network.models.MealPlanDraft
import com.example.wao_fe.network.models.MealPlanResponse
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

sealed class MealPlanState {
    object Idle : MealPlanState()
    object Loading : MealPlanState()
    data class SuggestionReady(val data: MealPlanResponse) : MealPlanState()
    data class DraftReady(val draft: MealPlanDraft) : MealPlanState()
    object SavingDraft : MealPlanState()
    data class DraftSaved(val data: MealPlanResponse) : MealPlanState()
    data class Error(val message: String) : MealPlanState()
}

class MealPlanViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _state = MutableLiveData<MealPlanState>(MealPlanState.Idle)
    val state: LiveData<MealPlanState> = _state

    private var currentSuggestion: MealPlanResponse? = null
    private var currentDraft: MealPlanDraft? = null
    private var editingMealPlanId: Long? = null

    fun generateMealPlan(userId: Long) {
        _state.value = MealPlanState.Loading
        viewModelScope.launch {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            when (val result = userRepository.generateMealPlan(userId, dateStr)) {
                is ApiResult.Success -> {
                    currentSuggestion = result.data
                    currentDraft = null
                    editingMealPlanId = null
                    _state.value = MealPlanState.SuggestionReady(result.data)
                }
                is ApiResult.Error -> {
                    _state.value = MealPlanState.Error(result.message)
                }
            }
        }
    }

    fun applySuggestionToDraft(userId: Long) {
        val suggestion = currentSuggestion
        if (suggestion == null) {
            _state.value = MealPlanState.Error("Bạn cần gợi ý AI trước khi áp dụng")
            return
        }

        val draftDate = SimpleDateFormat("dd/MM", Locale.forLanguageTag("vi-VN")).format(Date())
        val draft = MealPlanDraft(
            name = "Thực đơn AI $draftDate",
            description = "Bản nháp được tạo từ AI gợi ý",
            userId = userId,
            previewFoods = suggestion.foods
        )
        currentDraft = draft
        _state.value = MealPlanState.DraftReady(draft)
    }

    fun saveDraftMealPlan() {
        val draft = currentDraft
        if (draft == null) {
            _state.value = MealPlanState.Error("Chưa có bản nháp để lưu")
            return
        }

        _state.value = MealPlanState.SavingDraft
        viewModelScope.launch {
            val result = editingMealPlanId?.let { mealPlanId ->
                userRepository.updateMealPlan(mealPlanId, draft.toRequest())
            } ?: userRepository.createMealPlan(draft.toRequest())

            when (result) {
                is ApiResult.Success -> {
                    editingMealPlanId = null
                    _state.value = MealPlanState.DraftSaved(result.data)
                }
                is ApiResult.Error -> {
                    _state.value = MealPlanState.Error(result.message)
                }
            }
        }
    }

    fun editSavedMealPlan(
        userId: Long,
        mealPlan: MealPlanResponse,
        nameOverride: String? = null,
        descriptionOverride: String? = null
    ) {
        val draftName = nameOverride?.trim().takeUnless { it.isNullOrEmpty() } ?: mealPlan.name
        val draftDescription = if (nameOverride != null || descriptionOverride != null) {
            descriptionOverride?.trim()?.ifEmpty { null }
        } else {
            mealPlan.description ?: "Bản nháp điều chỉnh từ Meal Plan đã lưu"
        }

        val draft = MealPlanDraft(
            name = draftName,
            description = draftDescription,
            userId = userId,
            previewFoods = mealPlan.foods
        )
        currentSuggestion = mealPlan
        currentDraft = draft
        editingMealPlanId = mealPlan.id
        _state.value = MealPlanState.DraftReady(draft)
    }

    fun resetState() {
        currentSuggestion = null
        currentDraft = null
        editingMealPlanId = null
        _state.value = MealPlanState.Idle
    }
}
