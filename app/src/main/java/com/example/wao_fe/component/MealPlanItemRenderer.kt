package com.example.wao_fe.component

import android.view.LayoutInflater
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.wao_fe.R
import com.example.wao_fe.network.models.MealPlanResponse

enum class MealPlanItemMode {
    VIEW_ONLY,
    MANAGE
}

object MealPlanItemRenderer {

    fun render(
        container: LinearLayout,
        mealPlans: List<MealPlanResponse>,
        mode: MealPlanItemMode,
        onViewDetail: (MealPlanResponse) -> Unit,
        onAdjust: ((MealPlanResponse) -> Unit)? = null,
        onDelete: ((MealPlanResponse) -> Unit)? = null,
        onApply: ((MealPlanResponse) -> Unit)? = null
    ) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(container.context)

        mealPlans.forEach { mealPlan ->
            val layoutId = when (mode) {
                MealPlanItemMode.VIEW_ONLY -> R.layout.item_saved_plan
                MealPlanItemMode.MANAGE -> R.layout.item_user_meal_plan_manage
            }

            val itemView = inflater.inflate(layoutId, container, false)

            val tvName = itemView.findViewById<TextView>(R.id.tvMealPlanName)
            val tvMeta = itemView.findViewById<TextView>(R.id.tvMealPlanMeta)
            val tvFoodsCount = itemView.findViewById<TextView>(R.id.tvFoodsCount)

            tvName.text = mealPlan.name
            tvMeta.text = "Loại: ${mealPlan.type.name}"
            tvFoodsCount.text = "${mealPlan.foods.size} món ăn"

            when (mode) {
                MealPlanItemMode.VIEW_ONLY -> {
                    itemView.setOnClickListener { onViewDetail(mealPlan) }
                }
                MealPlanItemMode.MANAGE -> {
                    itemView.findViewById<Button>(R.id.btnViewDetail).setOnClickListener {
                        onViewDetail(mealPlan)
                    }
                    itemView.findViewById<Button>(R.id.btnAdjust).setOnClickListener {
                        onAdjust?.invoke(mealPlan)
                    }
                    itemView.findViewById<Button>(R.id.btnDelete).setOnClickListener {
                        onDelete?.invoke(mealPlan)
                    }
                    itemView.findViewById<Button>(R.id.btnApplyToDiary).setOnClickListener {
                        onApply?.invoke(mealPlan)
                    }
                }
            }

            container.addView(itemView)
        }
    }
}



