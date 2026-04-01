package com.example.wao_fe.component

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.wao_fe.FoodSearchActivity
import com.example.wao_fe.R
import com.example.wao_fe.namstats.WeightLogUpdateActivity
import com.example.wao_fe.network.models.MealType

object FloatingAddMenu {

    fun create(
        activity: AppCompatActivity,
        onScanBarcode: (() -> Unit)? = null,
        onCreateFood: (() -> Unit)? = null
    ): Dialog {
        val dialog = Dialog(activity)
        val view = activity.layoutInflater.inflate(R.layout.layout_bottom_sheet_add, null)
        dialog.setContentView(view)

        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        dialog.window?.setDimAmount(0.6f)

        bindClick(
            parent = view,
            buttonId = R.id.btnSearchFood,
            dialog = dialog,
            action = {
                if (activity is FoodSearchActivity) {
                    Toast.makeText(activity, "Ban dang o trang tim mon", Toast.LENGTH_SHORT).show()
                    return@bindClick
                }

                val userId = activity
                    .getSharedPreferences("AppPrefs", AppCompatActivity.MODE_PRIVATE)
                    .getLong("USER_ID", -1L)

                val intent = Intent(activity, FoodSearchActivity::class.java).apply {
                    if (userId != -1L) {
                        putExtra(FoodSearchActivity.Companion.EXTRA_USER_ID, userId)
                    }
                    putExtra(FoodSearchActivity.Companion.EXTRA_MEAL_TYPE, MealType.BREAKFAST.name)
                }
                activity.startActivity(intent)
            }
        )
        bindClick(
            parent = view,
            buttonId = R.id.btnScanBarcode,
            dialog = dialog,
            action = {
                if (onScanBarcode != null) {
                    onScanBarcode.invoke()
                } else {
                    Toast.makeText(activity, "Chua cau hinh quet ma vach", Toast.LENGTH_SHORT).show()
                }
            }
        )
        bindClick(
            parent = view,
            buttonId = R.id.btnVoiceNote,
            dialog = dialog,
            action = {
                Toast.makeText(activity, "Ghi bang giong noi", Toast.LENGTH_SHORT).show()
            }
        )
        bindClick(
            parent = view,
            buttonId = R.id.btnLogWater,
            dialog = dialog,
            action = {
                Toast.makeText(activity, "Uong nuoc", Toast.LENGTH_SHORT).show()
            }
        )
        bindClick(
            parent = view,
            buttonId = R.id.btnLogActivity,
            dialog = dialog,
            action = {
                Toast.makeText(activity, "Ghi lai hoat dong", Toast.LENGTH_SHORT).show()
            }
        )
        bindClick(
            parent = view,
            buttonId = R.id.btnLogWeight,
            dialog = dialog,
            action = {
                //namthem
                activity.startActivity(WeightLogUpdateActivity.createIntent(activity))
            }
        )
        bindClick(
            parent = view,
            buttonId = R.id.btnCreateRecipe,
            dialog = dialog,
            action = {
                Toast.makeText(activity, "Tao cong thuc", Toast.LENGTH_SHORT).show()
            }
        )
        bindClick(
            parent = view,
            buttonId = R.id.btnCreateFood,
            dialog = dialog,
            action = {
                if (onCreateFood != null) {
                    onCreateFood.invoke()
                } else {
                    Toast.makeText(activity, "Tao thuc pham", Toast.LENGTH_SHORT).show()
                }
            }
        )

        return dialog
    }

    private fun bindClick(parent: View, buttonId: Int, dialog: Dialog, action: () -> Unit) {
        parent.findViewById<View>(buttonId).setOnClickListener {
            dialog.dismiss()
            action.invoke()
        }
    }
}
