package com.example.wao_fe.network.models

// Common error shape returned by GlobalExceptionHandler.
data class ApiErrorResponse(
    val status: Int,
    val message: String,
    val fieldErrors: Map<String, String> = emptyMap(),
    val timestamp: String? = null
)

enum class UserStatus { ACTIVE, INACTIVE, BANNED }
enum class Gender { MALE, FEMALE, OTHER }
enum class ActivityLevel { SEDENTARY, LIGHTLY_ACTIVE, MODERATELY_ACTIVE, VERY_ACTIVE, EXTRA_ACTIVE }
enum class GoalType { LOSE_WEIGHT, GAIN_WEIGHT, MAINTAIN }
enum class MealType { BREAKFAST, LUNCH, DINNER, SNACK }
enum class MealPlanType { SYSTEM_SUGGESTION, USER_CUSTOM }
enum class ProgramLevel { BEGINNER, INTERMEDIATE, PRO }

// Users
data class GoogleLoginRequest(
    val idToken: String
)

data class VerifyEmailRequest(
    val email: String,
    val code: String
)

data class VerifyEmailResponse(
    val message: String,
    val userId: Long? = null
)

data class RegisterUserRequest(
    val email: String,
    val password: String,
    val fullName: String
)

data class UpdateUserRequest(
    val fullName: String? = null,
    val status: UserStatus? = null
)

data class UserResponse(
    val id: Long,
    val email: String,
    val fullName: String,
    val status: UserStatus
)

// Health profile
data class CreateHealthProfileRequest(
    val gender: Gender,
    val dob: String,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: ActivityLevel,
    val goalType: GoalType,
    val desiredWeightKg: Double,
    val targetDays: Int,
    val allergies: String? = null,
    val preferenceVector: String? = null
)

data class DailyCalorieBreakdownResponse(
    val dailyCalories: Double,
    val difficultyLevel: String,
    val note: String
)

data class HealthProfileResponse(
    val id: Long,
    val userId: Long,
    val gender: Gender,
    val dob: String,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: ActivityLevel,
    val goalType: GoalType,
    val desiredWeightKg: Double,
    val targetDays: Int,
    val targetCalories: Double,
    val dailyCalories: Double,
    val dailyCalorieBreakdown: DailyCalorieBreakdownResponse,
    val allergies: String? = null,
    val preferenceVector: String? = null,
    val createdAt: String? = null
)

// Food
data class FoodRequest(
    val name: String,
    val servingSize: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

data class FoodResponse(
    val id: Long,
    val name: String,
    val servingSize: String,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val isVerified: Boolean,
    val imageUrls: List<String> = emptyList(),
    val ingredients: String? = null,
    val containsAllergens: String? = null
)

// Food logs
data class CreateFoodLogRequest(
    val foodId: Long,
    val mealType: MealType,
    val servingQty: Double,
    val logDate: String
)

data class FoodLogResponse(
    val id: Long,
    val userId: Long,
    val foodId: Long,
    val foodName: String,
    val mealType: MealType,
    val servingQty: Double,
    val totalCalories: Double,
    val logDate: String
)

// Exercise
data class ExerciseRequest(
    val name: String,
    val categoryId: Long,
    val videoUrl: String? = null,
    val caloriesPerMin: Double,
    val description: String? = null
)

data class ExerciseResponse(
    val id: Long,
    val name: String,
    val categoryId: Long,
    val videoUrl: String? = null,
    val caloriesPerMin: Double,
    val description: String? = null
)

// Workout program
data class WorkoutProgramExerciseRequest(
    val exerciseId: Long,
    val orderIndex: Int,
    val sets: Int,
    val reps: Int,
    val restTimeSec: Int
)

data class WorkoutProgramRequest(
    val name: String,
    val level: ProgramLevel,
    val estimatedDuration: Int,
    val description: String? = null,
    val exercises: List<WorkoutProgramExerciseRequest>
)

data class WorkoutProgramExerciseResponse(
    val exerciseId: Long,
    val exerciseName: String? = null,
    val orderIndex: Int,
    val sets: Int,
    val reps: Int,
    val restTimeSec: Int
)

data class WorkoutProgramResponse(
    val id: Long,
    val name: String,
    val level: ProgramLevel,
    val estimatedDuration: Int,
    val description: String? = null,
    val exercises: List<WorkoutProgramExerciseResponse> = emptyList()
)

// Meal plan
data class MealPlanFoodRequest(
    val foodId: Long,
    val mealType: MealType,
    val servingQty: Double
)

data class MealPlanRequest(
    val name: String,
    val description: String? = null,
    val type: MealPlanType,
    val userId: Long? = null,
    val foods: List<MealPlanFoodRequest>
)

data class MealPlanFoodResponse(
    val foodId: Long,
    val foodName: String? = null,
    val mealType: MealType,
    val servingQty: Double,
    val calories: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val containsAllergens: String? = null
)

data class MealPlanResponse(
    val id: Long,
    val name: String,
    val description: String? = null,
    val type: MealPlanType,
    val userId: Long? = null,
    val foods: List<MealPlanFoodResponse> = emptyList()
)

data class MealPlanDraft(
    val name: String,
    val description: String? = null,
    val userId: Long,
    val previewFoods: List<MealPlanFoodResponse> = emptyList()
) {
    fun toRequest(): MealPlanRequest {
        return MealPlanRequest(
            name = name,
            description = description,
            type = MealPlanType.USER_CUSTOM,
            userId = userId,
            foods = previewFoods.map {
                MealPlanFoodRequest(
                    foodId = it.foodId,
                    mealType = it.mealType,
                    servingQty = it.servingQty
                )
            }
        )
    }
}

data class ApplyMealPlanRequest(
    val userId: Long,
    val logDate: String,
    val transientFoods: List<MealPlanFoodRequest>? = null
)

// Workout logs
data class CreateWorkoutLogRequest(
    val exerciseId: Long? = null,
    val programId: Long? = null,
    val durationMin: Int,
    val caloriesBurned: Double? = null,
    val logDate: String,
    val note: String? = null
)

data class WorkoutLogResponse(
    val id: Long,
    val userId: Long,
    val exerciseId: Long? = null,
    val programId: Long? = null,
    val durationMin: Int,
    val caloriesBurned: Double,
    val logDate: String,
    val note: String? = null
)

// Step logs
data class CreateStepLogRequest(
    val stepCount: Int,
    val logDate: String
)

data class StepLogResponse(
    val id: Long,
    val userId: Long,
    val stepCount: Int,
    val logDate: String
)

// Water logs
data class CreateWaterLogRequest(
    val amountMl: Int,
    val logTime: String
)

data class WaterLogResponse(
    val id: Long,
    val userId: Long,
    val amountMl: Int,
    val logTime: String,
    val logDate: String
)

// Daily summary
data class DailySummaryResponse(
    val userId: Long,
    val logDate: String,
    val totalCalIn: Double,
    val totalCalOut: Double,
    val netCalories: Double,
    val totalWater: Int,
    val totalSteps: Int,
    val isGoalAchieved: Boolean
)

// Chatbot
data class ChatbotSendMessageRequest(
    val conversationId: Long? = null,
    val message: String
)

data class ChatbotSendMessageResponse(
    val conversationId: Long,
    val assistantMessageId: Long,
    val answer: String,
    val createdAt: String
)

data class ChatbotConversationSummary(
    val id: Long,
    val title: String,
    val model: String,
    val createdAt: String,
    val updatedAt: String
)

data class ChatbotMessageItem(
    val id: Long,
    val role: String,
    val content: String,
    val totalTokens: Int? = null,
    val createdAt: String? = null
)

data class ChatbotConversationDetail(
    val conversationId: Long,
    val title: String,
    val model: String,
    val messages: List<ChatbotMessageItem> = emptyList()
)
