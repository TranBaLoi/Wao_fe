package com.example.wao_fe

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.wao_fe.namstats.models.CreateWeightLogRequest
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.UserRepository
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale

class UpdateWeightBottomSheet : BottomSheetDialogFragment() {

    private val userRepository = UserRepository()
    private val selectedDate: Calendar = Calendar.getInstance()
    private var currentWeight = 51.0
    private var userId: Long = -1L

    var onWeightUpdated: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = arguments?.getLong(ARG_USER_ID, -1L) ?: -1L
        if (userId == -1L) {
            userId = requireContext().getSharedPreferences("AppPrefs", android.content.Context.MODE_PRIVATE)
                .getLong("USER_ID", -1L)
        }
    }

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

        tvDateDisplay?.text = formatDisplayDate(selectedDate)

        btnClose?.setOnClickListener {
            dismiss()
        }

        fun updateWeightText() {
            tvCurrentWeight?.text = "${formatWeight(currentWeight)} Kg"
        }

        updateWeightText()

        btnMinus?.setOnClickListener {
            if (currentWeight > 1.0) {
                currentWeight -= 0.5
                updateWeightText()
            }
        }

        btnPlus?.setOnClickListener {
            currentWeight += 0.5
            updateWeightText()
        }

        layoutDate?.setOnClickListener {
            val year = selectedDate.get(Calendar.YEAR)
            val month = selectedDate.get(Calendar.MONTH)
            val day = selectedDate.get(Calendar.DAY_OF_MONTH)

            val dpd = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate.set(selectedYear, selectedMonth, selectedDay)
                tvDateDisplay?.text = formatDisplayDate(selectedDate)
            }, year, month, day)
            dpd.show()
        }

        btnSaveWeight?.setOnClickListener {
            if (userId == -1L) {
                Toast.makeText(requireContext(), "Không tìm thấy thông tin đăng nhập", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnSaveWeight.isEnabled = false
            viewLifecycleOwner.lifecycleScope.launch {
                val result = userRepository.createWeightLog(
                    userId = userId,
                    request = CreateWeightLogRequest(
                        date = formatApiDate(selectedDate),
                        newWeight = currentWeight
                    )
                )

                btnSaveWeight.isEnabled = true
                if (result is ApiResult.Success) {
                    Toast.makeText(requireContext(), "Cập nhật cân nặng thành công", Toast.LENGTH_SHORT).show()
                    onWeightUpdated?.invoke()
                    dismiss()
                } else {
                    val errorMessage = (result as? ApiResult.Error)?.message ?: "Không thể cập nhật cân nặng"
                    Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
        }

        if (userId != -1L) {
            viewLifecycleOwner.lifecycleScope.launch {
                val latestResult = userRepository.getLatestWeightInfo(userId)
                if (latestResult is ApiResult.Success) {
                    latestResult.data.latestKnownWeight?.let {
                        currentWeight = it
                        updateWeightText()
                    }
                }
            }
        }
    }

    private fun formatDisplayDate(calendar: Calendar): String {
        return SimpleDateFormat("dd / MM / yyyy", Locale.getDefault()).format(calendar.time)
    }

    private fun formatApiDate(calendar: Calendar): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun formatWeight(value: Double): String {
        return String.format(Locale.getDefault(), "%.1f", value)
    }

    companion object {
        private const val ARG_USER_ID = "arg_user_id"

        fun newInstance(userId: Long): UpdateWeightBottomSheet {
            return UpdateWeightBottomSheet().apply {
                arguments = Bundle().apply {
                    putLong(ARG_USER_ID, userId)
                }
            }
        }
    }
}

