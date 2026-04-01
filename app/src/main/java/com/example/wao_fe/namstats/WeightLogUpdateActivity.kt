//namthem
package com.example.wao_fe.namstats

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wao_fe.R
import com.example.wao_fe.namstats.models.CreateWeightLogRequest
import com.example.wao_fe.namstats.models.LatestWeightInfoResponse
import com.example.wao_fe.network.NetworkClient
import com.google.gson.Gson
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

//namthem
class WeightLogUpdateActivity : AppCompatActivity() {

    private val apiService = NetworkClient.apiService
    @RequiresApi(Build.VERSION_CODES.O)
    private val dayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())

    @RequiresApi(Build.VERSION_CODES.O)
    private var selectedDate: LocalDate = LocalDate.now()
    private var userId: Long = -1L

    private lateinit var btnBack: ImageButton
    private lateinit var btnChooseDate: Button
    private lateinit var etNewWeight: EditText
    private lateinit var etNote: EditText
    private lateinit var btnSave: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weight_log_update)

        userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getLong("USER_ID", -1L)
        if (userId == -1L) {
            Toast.makeText(this, "Không tìm thấy thông tin đăng nhập", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        bindViews()
        setupControls()
        updateDateButton()
        loadLatestWeightInfo()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btn_back_weight_log)
        btnChooseDate = findViewById(R.id.btn_choose_weight_date)
        etNewWeight = findViewById(R.id.et_new_weight)
        etNote = findViewById(R.id.et_weight_note)
        btnSave = findViewById(R.id.btn_save_weight_log)
        progressBar = findViewById(R.id.progress_weight_log)
        tvStatus = findViewById(R.id.tv_weight_log_status)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupControls() {
        btnBack.setOnClickListener { finish() }
        btnChooseDate.setOnClickListener {
            //namthem
            Toast.makeText(this, "Chỉ hỗ trợ cập nhật cho ngày hiện tại", Toast.LENGTH_SHORT).show()
        }
        btnSave.setOnClickListener { submitWeightLog() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateDateButton() {
        //namthem
        selectedDate = LocalDate.now()
        btnChooseDate.text = "Ngày hiện tại: ${selectedDate.format(dayFormatter)}"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun submitWeightLog() {
        val newWeight = etNewWeight.text.toString().trim().toDoubleOrNull()
        if (newWeight == null || newWeight <= 0.0) {
            etNewWeight.error = "Nhập cân nặng hợp lệ"
            return
        }

        progressBar.visibility = android.view.View.VISIBLE
        btnSave.isEnabled = false
        tvStatus.text = "Đang lưu cân nặng"

        lifecycleScope.launch {
            runCatching {
                apiService.createWeightLog(
                    userId = userId,
                    request = CreateWeightLogRequest(
                        date = selectedDate.toString(),
                        newWeight = newWeight,
                        note = etNote.text.toString().trim().ifBlank { null }
                    )
                )
            }.onSuccess { response ->
                progressBar.visibility = android.view.View.GONE
                btnSave.isEnabled = true
                tvStatus.text = buildLatestWeightText(
                    latestWeight = response.newWeight,
                    latestDate = response.date
                )
                Toast.makeText(this@WeightLogUpdateActivity, "Lưu cân nặng thành công", Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }.onFailure { error ->
                progressBar.visibility = android.view.View.GONE
                btnSave.isEnabled = true
                //namthem
                tvStatus.text = extractErrorMessage(error)
                Toast.makeText(this@WeightLogUpdateActivity, tvStatus.text, Toast.LENGTH_LONG).show()
            }
        }
    }

    //namthem
    private fun loadLatestWeightInfo() {
        tvStatus.text = "Đang tải cân nặng gần nhất"
        lifecycleScope.launch {
            runCatching {
                apiService.getLatestWeightInfo(userId)
            }.onSuccess { response ->
                tvStatus.text = buildLatestWeightText(response)
            }.onFailure {
                tvStatus.text = "Chưa lấy được cân nặng gần nhất"
            }
        }
    }

    //namthem
    private fun buildLatestWeightText(response: LatestWeightInfoResponse): String {
        return buildLatestWeightText(
            latestWeight = response.latestKnownWeight,
            latestDate = response.latestKnownDate
        )
    }

    //namthem
    private fun buildLatestWeightText(latestWeight: Double?, latestDate: String?): String {
        if (latestWeight == null) {
            return "Chưa có dữ liệu cân nặng gần nhất"
        }
        val dateText = latestDate?.let { raw ->
            runCatching {
                LocalDate.parse(raw).format(dayFormatter)
            }.getOrDefault(raw)
        } ?: "không rõ ngày"
        return "Cân nặng gần nhất: ${String.format(Locale.getDefault(), "%.1f", latestWeight)} kg vào ngày $dateText"
    }

    //namthem
    private fun extractErrorMessage(error: Throwable): String {
        if (error is HttpException) {
            val rawBody = error.response()?.errorBody()?.string()
            if (!rawBody.isNullOrBlank()) {
                val parsed = runCatching {
                    Gson().fromJson(rawBody, ErrorBody::class.java)
                }.getOrNull()
                if (!parsed?.message.isNullOrBlank()) {
                    return parsed!!.message
                }
            }
        }
        return error.message ?: "Lưu cân nặng thất bại"
    }

    //namthem
    data class ErrorBody(
        val status: Int? = null,
        val message: String? = null
    )

    companion object {
        //namthem
        fun createIntent(context: Context): Intent {
            return Intent(context, WeightLogUpdateActivity::class.java)
        }
    }
}
