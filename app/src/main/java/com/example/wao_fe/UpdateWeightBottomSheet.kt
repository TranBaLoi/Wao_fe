package com.example.wao_fe

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Calendar

class UpdateWeightBottomSheet : BottomSheetDialogFragment() {

    private var currentWeight = 51

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_update_weight, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnClose = view.findViewById<ImageView>(R.id.btnClose)
        val tvCurrentWeight = view.findViewById<TextView>(R.id.tvCurrentWeight)
        val btnMinus = view.findViewById<ImageView>(R.id.btnMinus)
        val btnPlus = view.findViewById<ImageView>(R.id.btnPlus)
        val tvDateDisplay = view.findViewById<TextView>(R.id.tvDateDisplay)
        val layoutDate = view.findViewById<View>(R.id.layoutDate)
        val btnSaveWeight = view.findViewById<Button>(R.id.btnSaveWeight)

        btnClose?.setOnClickListener {
            dismiss()
        }

        fun updateWeightText() {
            tvCurrentWeight?.text = "$currentWeight Kg"
        }

        btnMinus?.setOnClickListener {
            if (currentWeight > 1) {
                currentWeight -= 1
                updateWeightText()
            }
        }

        btnPlus?.setOnClickListener {
            currentWeight += 1
            updateWeightText()
        }

        layoutDate?.setOnClickListener {
            val c = Calendar.getInstance()
            val year = c.get(Calendar.YEAR)
            val month = c.get(Calendar.MONTH)
            val day = c.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                val dateStr = String.format("%02d / %02d / %04d", selectedDay, selectedMonth + 1, selectedYear)
                tvDateDisplay?.text = dateStr
            }, year, month, day)
            dpd.show()
        }

        btnSaveWeight?.setOnClickListener {
            // Save logic...
            dismiss()
        }
    }
}

