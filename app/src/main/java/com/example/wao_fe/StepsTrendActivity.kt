package com.example.wao_fe

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import com.example.wao_fe.health.DailyStepsTimeline
import com.example.wao_fe.health.HealthConnectManager
import com.example.wao_fe.health.HealthConnectRepository
import com.example.wao_fe.namstats.views.NamTrendChartView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class StepsTrendActivity : AppCompatActivity() {

    private enum class HealthActionState { NONE, REQUEST_PERMISSION, INSTALL_APP }

    private lateinit var btnBack: ImageButton
    private lateinit var tvStatus: TextView
    private lateinit var progressSteps: ProgressBar
    private lateinit var btnToday: MaterialButton
    private lateinit var btnYesterday: MaterialButton
    private lateinit var tvSelectedDayTitle: TextView
    private lateinit var tvSelectedDaySubtitle: TextView
    private lateinit var tvSelectedDayTotalSteps: TextView
    private lateinit var tvHealthConnectHint: TextView
    private lateinit var btnHealthConnectAction: MaterialButton
    private lateinit var chartView: NamTrendChartView
    private lateinit var tvBucketTitle: TextView
    private lateinit var tvBucketSubtitle: TextView
    private lateinit var tvBucketMetricOne: TextView
    private lateinit var tvBucketMetricTwo: TextView
    private lateinit var tvBucketMetricThree: TextView
    private lateinit var tvBucketMetricFour: TextView

    private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("vi", "VN"))
    private var selectedDate: LocalDate = LocalDate.now()
    private var currentTimeline: DailyStepsTimeline? = null
    private var currentHealthAction = HealthActionState.NONE

    private val requestHealthPermissions = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        if (granted.containsAll(HealthConnectManager.stepReadPermissions)) {
            Toast.makeText(this, "Da ket noi Health Connect", Toast.LENGTH_SHORT).show()
            loadSelectedDay()
        } else {
            Toast.makeText(this, "Chua cap du quyen doc du lieu buoc chan", Toast.LENGTH_SHORT).show()
            renderPermissionState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_steps_trend)

        bindViews()
        bindActions()
        updateDayButtons()
        loadSelectedDay()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btnBackStepsTrend)
        tvStatus = findViewById(R.id.tvStepsTrendStatus)
        progressSteps = findViewById(R.id.progressStepsTrend)
        btnToday = findViewById(R.id.btnStepsToday)
        btnYesterday = findViewById(R.id.btnStepsYesterday)
        tvSelectedDayTitle = findViewById(R.id.tvStepsTrendDayTitle)
        tvSelectedDaySubtitle = findViewById(R.id.tvStepsTrendDaySubtitle)
        tvSelectedDayTotalSteps = findViewById(R.id.tvStepsTrendDayTotalSteps)
        tvHealthConnectHint = findViewById(R.id.tvStepsTrendHealthHint)
        btnHealthConnectAction = findViewById(R.id.btnStepsTrendHealthAction)
        chartView = findViewById(R.id.chartStepsTrend)
        tvBucketTitle = findViewById(R.id.tvStepsTrendBucketTitle)
        tvBucketSubtitle = findViewById(R.id.tvStepsTrendBucketSubtitle)
        tvBucketMetricOne = findViewById(R.id.tvStepsTrendBucketMetricOne)
        tvBucketMetricTwo = findViewById(R.id.tvStepsTrendBucketMetricTwo)
        tvBucketMetricThree = findViewById(R.id.tvStepsTrendBucketMetricThree)
        tvBucketMetricFour = findViewById(R.id.tvStepsTrendBucketMetricFour)
    }

    private fun bindActions() {
        btnBack.setOnClickListener { finish() }
        btnToday.setOnClickListener {
            selectedDate = LocalDate.now()
            updateDayButtons()
            loadSelectedDay()
        }
        btnYesterday.setOnClickListener {
            selectedDate = LocalDate.now().minusDays(1)
            updateDayButtons()
            loadSelectedDay()
        }
        btnHealthConnectAction.setOnClickListener {
            when (currentHealthAction) {
                HealthActionState.NONE -> Unit
                HealthActionState.REQUEST_PERMISSION -> requestHealthPermissions.launch(HealthConnectManager.stepReadPermissions)
                HealthActionState.INSTALL_APP -> openHealthConnectStore()
            }
        }
        chartView.setOnPointSelectedListener { index ->
            currentTimeline?.let { timeline ->
                renderBucketDetails(timeline, index)
            }
        }
    }

    private fun updateDayButtons() {
        val isTodaySelected = selectedDate == LocalDate.now()
        setDayButtonActive(btnToday, isTodaySelected)
        setDayButtonActive(btnYesterday, !isTodaySelected)
    }

    private fun setDayButtonActive(button: MaterialButton, isActive: Boolean) {
        if (isActive) {
            button.setBackgroundColor(getColor(R.color.wao_primary))
            button.setTextColor(getColor(R.color.wao_background_dark))
        } else {
            button.setBackgroundColor(getColor(R.color.wao_slate_100))
            button.setTextColor(getColor(R.color.wao_slate_600))
        }
    }

    private fun loadSelectedDay() {
        progressSteps.visibility = View.VISIBLE
        tvStatus.text = "Dang tai du lieu buoc chan"

        lifecycleScope.launch {
            when (HealthConnectManager.getSdkStatus(this@StepsTrendActivity)) {
                HealthConnectClient.SDK_AVAILABLE -> {
                    val client = HealthConnectClient.getOrCreate(this@StepsTrendActivity)
                    val hasAccess = client.permissionController.getGrantedPermissions()
                        .containsAll(HealthConnectManager.stepReadPermissions)
                    if (!hasAccess) {
                        renderPermissionState()
                        return@launch
                    }

                    runCatching {
                        HealthConnectRepository(client).readDailyStepsTimeline(selectedDate)
                    }.onSuccess { timeline ->
                        renderTimeline(timeline)
                    }.onFailure { error ->
                        progressSteps.visibility = View.GONE
                        tvStatus.text = "Khong tai duoc du lieu so buoc"
                        tvHealthConnectHint.text = error.message ?: "Khong the doc du lieu tu Health Connect"
                        btnHealthConnectAction.visibility = View.GONE
                    }
                }

                else -> {
                    renderInstallState()
                }
            }
        }
    }

    private fun renderPermissionState() {
        progressSteps.visibility = View.GONE
        currentTimeline = null
        currentHealthAction = HealthActionState.REQUEST_PERMISSION
        tvStatus.text = "Can Health Connect de xem bieu do so buoc"
        tvSelectedDayTitle.text = selectedDayLabel()
        tvSelectedDaySubtitle.text = selectedDate.format(dateFormatter)
        tvSelectedDayTotalSteps.text = "--"
        tvHealthConnectHint.text = "Hay cap quyen doc Steps trong Health Connect de xem du lieu hom nay va hom qua."
        btnHealthConnectAction.visibility = View.VISIBLE
        btnHealthConnectAction.text = "Ket noi Health Connect"
        chartView.submitData(emptyList(), emptyList())
        renderEmptyBucketDetails()
    }

    private fun renderInstallState() {
        progressSteps.visibility = View.GONE
        currentTimeline = null
        currentHealthAction = HealthActionState.INSTALL_APP
        tvStatus.text = "Thiet bi chua san sang Health Connect"
        tvSelectedDayTitle.text = selectedDayLabel()
        tvSelectedDaySubtitle.text = selectedDate.format(dateFormatter)
        tvSelectedDayTotalSteps.text = "--"
        tvHealthConnectHint.text = "Can cai dat Health Connect de doc du lieu buoc chan tren may."
        btnHealthConnectAction.visibility = View.VISIBLE
        btnHealthConnectAction.text = "Mo Play Store"
        chartView.submitData(emptyList(), emptyList())
        renderEmptyBucketDetails()
    }

    private fun renderTimeline(timeline: DailyStepsTimeline) {
        progressSteps.visibility = View.GONE
        currentTimeline = timeline
        currentHealthAction = HealthActionState.NONE
        tvStatus.text = "Da dong bo du lieu buoc chan tu Health Connect"
        tvSelectedDayTitle.text = selectedDayLabel()
        tvSelectedDaySubtitle.text = timeline.date.format(dateFormatter)
        tvSelectedDayTotalSteps.text = formatSteps(timeline.totalSteps)
        tvHealthConnectHint.text = "Cham vao tung diem tren bieu do de xem chi tiet theo gio."
        btnHealthConnectAction.visibility = View.GONE

        val labels = timeline.buckets.map { it.label }
        val values = timeline.buckets.map { it.steps.toFloat() }
        chartView.setAxisUnits("(buoc)", "Gio")
        chartView.submitData(values, labels)

        val highlightedIndex = timeline.buckets
            .indices
            .maxByOrNull { index -> timeline.buckets[index].steps }
            ?: 0
        chartView.selectIndex(highlightedIndex)
        renderBucketDetails(timeline, highlightedIndex)
    }

    private fun renderBucketDetails(timeline: DailyStepsTimeline, index: Int) {
        val bucket = timeline.buckets.getOrNull(index) ?: return
        val peakBucket = timeline.buckets.maxByOrNull { it.steps }
        tvBucketTitle.text = "Khung gio ${bucket.label} - ${nextHourLabel(bucket.hour)}"
        tvBucketSubtitle.text = "${formatSteps(bucket.steps)} trong khung gio nay"
        tvBucketMetricOne.text = "Tong ngay: ${formatSteps(timeline.totalSteps)}"
        tvBucketMetricTwo.text = "Dinh trong ngay: ${peakBucket?.let { "${it.label} - ${nextHourLabel(it.hour)}" } ?: "--"}"
        tvBucketMetricThree.text = "So buoc cao nhat: ${peakBucket?.steps?.let(::formatSteps) ?: "--"}"
        tvBucketMetricFour.text = "Nguon du lieu: Health Connect"
    }

    private fun renderEmptyBucketDetails() {
        tvBucketTitle.text = "Chua co bieu do"
        tvBucketSubtitle.text = "Ket noi Health Connect de xem so buoc theo gio."
        tvBucketMetricOne.text = "Tong ngay: --"
        tvBucketMetricTwo.text = "Dinh trong ngay: --"
        tvBucketMetricThree.text = "So buoc cao nhat: --"
        tvBucketMetricFour.text = "Nguon du lieu: --"
    }

    private fun selectedDayLabel(): String {
        return if (selectedDate == LocalDate.now()) "Hom nay" else "Hom qua"
    }

    private fun nextHourLabel(hour: Int): String {
        return "%02d:00".format((hour + 1) % 24)
    }

    private fun formatSteps(steps: Long): String {
        return String.format(Locale.US, "%,d buoc", steps)
    }

    private fun openHealthConnectStore() {
        try {
            startActivity(HealthConnectManager.buildInstallIntent(this))
        } catch (_: ActivityNotFoundException) {
            startActivity(HealthConnectManager.buildBrowserFallbackIntent())
        }
    }
}
