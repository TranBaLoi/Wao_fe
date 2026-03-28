//nam them
package com.example.wao_fe.namstats

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.wao_fe.R
import com.example.wao_fe.namstats.views.NamTrendChartView
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class StatisticsDashboardActivity : AppCompatActivity() {

    private enum class StatisticsPeriod {
        DAY,
        WEEK,
        MONTH
    }

    private enum class ChartMetric {
        WEIGHT,
        PROTEIN,
        CARBS,
        FAT,
        CALORIES
    }

    private val repository = NamStatisticsRepository()
    private val dayFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())

    private var selectedPeriod = StatisticsPeriod.DAY
    private var selectedDate = LocalDate.now()
    private var selectedMetric = ChartMetric.CALORIES
    private var currentRangeSnapshot: RangeSnapshot? = null
    private var currentChartDates: List<String> = emptyList()
    //nam them
    private var currentChartValuesByDate: Map<String, Double> = emptyMap()
    private var isUpdatingMetricSpinner = false
    private var userId: Long = -1L

    private lateinit var progressBar: ProgressBar
    private lateinit var btnBack: ImageButton
    private lateinit var tvStatus: TextView
    private lateinit var btnPeriodDay: Button
    private lateinit var btnPeriodWeek: Button
    private lateinit var btnPeriodMonth: Button
    private lateinit var btnChooseDate: Button
    private lateinit var metricSpinner: Spinner
    private lateinit var tvSelectedRangeTitle: TextView
    private lateinit var tvSelectedRangeSubtitle: TextView
    private lateinit var chartCard: View
    private lateinit var chartHint: TextView
    private lateinit var chartView: NamTrendChartView
    private lateinit var detailContainer: LinearLayout
    private lateinit var bottomNavigationView: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nam_statistics_dashboard)
        //nam them
        userId = getSharedPreferences("AppPrefs", MODE_PRIVATE).getLong("USER_ID", -1L)

        bindViews()
        setupControls()
        updateControlState()
        if (userId == -1L) {
            Toast.makeText(this, "Không tìm thấy thông tin đăng nhập", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        loadSelectedContent()
    }

    private fun bindViews() {
        btnBack = findViewById(R.id.btn_back_statistics)
        progressBar = findViewById(R.id.progress_dashboard)
        tvStatus = findViewById(R.id.tv_dashboard_status)
        btnPeriodDay = findViewById(R.id.btn_period_day)
        btnPeriodWeek = findViewById(R.id.btn_period_week)
        btnPeriodMonth = findViewById(R.id.btn_period_month)
        btnChooseDate = findViewById(R.id.btn_choose_date)
        metricSpinner = findViewById(R.id.spinner_metric)
        tvSelectedRangeTitle = findViewById(R.id.tv_selected_range_title)
        tvSelectedRangeSubtitle = findViewById(R.id.tv_selected_range_subtitle)
        chartCard = findViewById(R.id.card_chart)
        //nam them
        chartHint = findViewById(R.id.tv_chart_hint)
        chartView = findViewById(R.id.chart_trend)
        detailContainer = findViewById(R.id.container_detail_items)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
    }

    private fun setupControls() {
        //nam them
        btnBack.setOnClickListener { finish() }
        btnChooseDate.setOnClickListener { openDatePicker() }

        btnPeriodDay.setOnClickListener {
            selectedPeriod = StatisticsPeriod.DAY
            loadSelectedContent()
        }
        btnPeriodWeek.setOnClickListener {
            selectedPeriod = StatisticsPeriod.WEEK
            loadSelectedContent()
        }
        btnPeriodMonth.setOnClickListener {
            selectedPeriod = StatisticsPeriod.MONTH
            loadSelectedContent()
        }

        setupMetricSpinner()

        chartView.setOnPointSelectedListener { index ->
            val snapshot = currentRangeSnapshot ?: return@setOnPointSelectedListener
            val dateKey = currentChartDates.getOrNull(index) ?: return@setOnPointSelectedListener
            renderRangeDayDetail(snapshot, dateKey)
        }
    }

    private fun setupMetricSpinner() {
        val items = ChartMetric.entries.map(::metricLabel)
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        metricSpinner.adapter = adapter
        metricSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isUpdatingMetricSpinner) return
                selectedMetric = ChartMetric.entries[position]
                updateMetricChart()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }

        bottomNavigationView.selectedItemId = R.id.nav_diary
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(android.content.Intent(this, com.example.wao_fe.MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_diary -> {
                    startActivity(android.content.Intent(this, com.example.wao_fe.FoodDiaryActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_menu -> {
                    startActivity(android.content.Intent(this, com.example.wao_fe.MealPlanActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(android.content.Intent(this, com.example.wao_fe.SettingsActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun loadSelectedContent() {
        updateControlState()
        progressBar.visibility = View.VISIBLE
        tvStatus.text = "Đang tải dữ liệu thống kê"

        lifecycleScope.launch {
            runCatching {
                when (selectedPeriod) {
                    StatisticsPeriod.DAY -> renderDaySection()
                    StatisticsPeriod.WEEK -> renderRangeSection(repository.buildWeekRange(selectedDate))
                    StatisticsPeriod.MONTH -> renderRangeSection(repository.buildMonthRange(selectedDate))
                }
            }.onFailure { error ->
                progressBar.visibility = View.GONE
                tvStatus.text = "Không tải dữ liệu được: ${error.message ?: "Lỗi không xác định"}"
                Toast.makeText(this@StatisticsDashboardActivity, error.message, Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun renderDaySection() {
        val snapshot = repository.loadDailySnapshot(userId, selectedDate)
        chartCard.visibility = View.GONE
        currentRangeSnapshot = null
        currentChartDates = emptyList()
        currentChartValuesByDate = emptyMap()

        renderDailySnapshot(snapshot)
        finishLoading()
    }

    private suspend fun renderRangeSection(range: DateRange) {
        val snapshot = repository.loadRangeSnapshot(userId, range)
        currentRangeSnapshot = snapshot
        chartCard.visibility = View.VISIBLE

        tvSelectedRangeTitle.text = if (selectedPeriod == StatisticsPeriod.WEEK) {
            "Thống kê theo tuần"
        } else {
            "Thống kê theo tháng"
        }
        tvSelectedRangeSubtitle.text = "Từ ngày ${formatDate(range.start)} đến ngày ${formatDate(range.end)}"

        updateMetricButtons()
        updateMetricChart()
        finishLoading()
    }

    private fun renderDailySnapshot(snapshot: DailySnapshot) {
        detailContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        val nutritionCard = inflater.inflate(R.layout.item_nam_stat_point, detailContainer, false)
        nutritionCard.findViewById<TextView>(R.id.tv_point_title).text = "Chi tiết ngày ${formatDate(snapshot.date)}"
        nutritionCard.findViewById<TextView>(R.id.tv_point_subtitle).text =
            "Tổng lượng calo: ${formatCalories(snapshot.nutrition.totalCalories)}"
        nutritionCard.findViewById<TextView>(R.id.tv_metric_one).text =
            "Protein: ${formatGram(snapshot.nutrition.totalProtein)}"
        nutritionCard.findViewById<TextView>(R.id.tv_metric_two).text =
            "Carbs: ${formatGram(snapshot.nutrition.totalCarbs)}"
        nutritionCard.findViewById<TextView>(R.id.tv_metric_three).text =
            "Fat: ${formatGram(snapshot.nutrition.totalFat)}"
        nutritionCard.findViewById<TextView>(R.id.tv_metric_four).text =
            "Cân nặng hiện tại: ${formatWeight(snapshot.currentWeight)}"
        detailContainer.addView(nutritionCard)

        val weightCard = inflater.inflate(R.layout.item_nam_stat_point, detailContainer, false)
        weightCard.findViewById<TextView>(R.id.tv_point_title).text = "Chi tiết cân nặng"
        weightCard.findViewById<TextView>(R.id.tv_point_subtitle).text =
            "Cân nặng hiện tại: ${formatWeight(snapshot.currentWeight)}"
        weightCard.findViewById<TextView>(R.id.tv_metric_one).text =
            "Cân nặng cũ: ${formatWeight(snapshot.previousWeight)}"
        weightCard.findViewById<TextView>(R.id.tv_metric_two).text =
            "Hiện tại: ${formatWeight(snapshot.currentWeight)}"
        weightCard.findViewById<TextView>(R.id.tv_metric_three).text =
            "Thay đổi: ${formatWeightChange(snapshot.weightChange)}"
        weightCard.findViewById<TextView>(R.id.tv_metric_four).text =
            if (snapshot.previousWeight == null && snapshot.fallbackWeight != null) {
                "Fallback từ profile: ${formatWeight(snapshot.fallbackWeight)}"
            } else {
                "Dữ liệu từ log cân nặng"
            }
        detailContainer.addView(weightCard)
    }

    private fun updateMetricButtons() {
        isUpdatingMetricSpinner = true
        metricSpinner.setSelection(ChartMetric.entries.indexOf(selectedMetric), false)
        isUpdatingMetricSpinner = false
    }

    private fun updateMetricChart() {
        val snapshot = currentRangeSnapshot ?: return
        updateMetricButtons()
        chartView.setAxisUnits(yAxisUnitFor(selectedMetric), "Ngày")

        val chartPoints: List<Pair<String, Float>> = when (selectedMetric) {
            //nam them
            ChartMetric.WEIGHT -> snapshot.weight.points
                .mapNotNull { point ->
                    point.endWeight?.takeIf { it != 0.0 }?.let { point.bucketDate to it.toFloat() }
                }

            ChartMetric.PROTEIN -> snapshot.nutrition.points
                .map { it.bucketDate to it.totalProtein.toFloat() }
                .filter { it.second != 0f }

            ChartMetric.CARBS -> snapshot.nutrition.points
                .map { it.bucketDate to it.totalCarbs.toFloat() }
                .filter { it.second != 0f }

            ChartMetric.FAT -> snapshot.nutrition.points
                .map { it.bucketDate to it.totalFat.toFloat() }
                .filter { it.second != 0f }

            ChartMetric.CALORIES -> snapshot.nutrition.points
                .map { it.bucketDate to it.totalCalories.toFloat() }
                .filter { it.second != 0f }
        }

        //nam them
        currentChartValuesByDate = chartPoints.associate { it.first to it.second.toDouble() }
        currentChartDates = chartPoints.map { it.first }
        chartView.submitData(chartPoints.map { it.second }, currentChartDates)

        if (currentChartDates.isNotEmpty()) {
            renderRangeDayDetail(snapshot, currentChartDates.first())
        } else {
            detailContainer.removeAllViews()
        }
    }

    private fun renderRangeDayDetail(snapshot: RangeSnapshot, dateKey: String) {
        val nutrition = snapshot.nutritionPointByDate(dateKey)
        //nam them
        val resolvedWeight = currentChartValuesByDate[dateKey] ?: snapshot.resolvedWeightByDate(dateKey)
        //nam them
        val weightPoint = snapshot.weightPointByDate(dateKey)
        //nam them
        val oldWeight = weightPoint?.startWeight
        //nam them
        val weightChange = weightPoint?.changeAmount ?: if (resolvedWeight != null && oldWeight != null) {
            resolvedWeight - oldWeight
        } else {
            null
        }
        detailContainer.removeAllViews()

        val view = LayoutInflater.from(this).inflate(R.layout.item_nam_stat_point, detailContainer, false)
        view.findViewById<TextView>(R.id.tv_point_title).text = formatRawDate(dateKey)
        view.findViewById<TextView>(R.id.tv_point_subtitle).text = when (selectedMetric) {
            ChartMetric.WEIGHT -> "Cân nặng: ${formatWeight(resolvedWeight)}"
            ChartMetric.PROTEIN -> "Protein: ${formatGram(nutrition?.totalProtein)}"
            ChartMetric.CARBS -> "Carbs: ${formatGram(nutrition?.totalCarbs)}"
            ChartMetric.FAT -> "Fat: ${formatGram(nutrition?.totalFat)}"
            ChartMetric.CALORIES -> "Tổng lượng calo: ${formatCalories(nutrition?.totalCalories)}"
        }

        if (selectedMetric == ChartMetric.WEIGHT) {
            //nam them
            view.findViewById<TextView>(R.id.tv_metric_one).text = "Cân nặng cũ: ${formatWeight(oldWeight)}"
            //nam them
            view.findViewById<TextView>(R.id.tv_metric_two).text = "Cân nặng hiện tại: ${formatWeight(resolvedWeight)}"
            //nam them
            view.findViewById<TextView>(R.id.tv_metric_three).text = "Thay đổi: ${formatWeightChange(weightChange)}"
            view.findViewById<TextView>(R.id.tv_metric_four).text = "Số lần log: ${weightPoint?.logCount ?: 0}"
        } else {
            view.findViewById<TextView>(R.id.tv_metric_one).text = "Protein: ${formatGram(nutrition?.totalProtein)}"
            view.findViewById<TextView>(R.id.tv_metric_two).text = "Carbs: ${formatGram(nutrition?.totalCarbs)}"
            view.findViewById<TextView>(R.id.tv_metric_three).text = "Fat: ${formatGram(nutrition?.totalFat)}"
            view.findViewById<TextView>(R.id.tv_metric_four).text =
                "Cân nặng hiện tại: ${formatWeight(resolvedWeight)}"
        }
        detailContainer.addView(view)
    }

    private fun openDatePicker() {
        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                loadSelectedContent()
            },
            selectedDate.year,
            selectedDate.monthValue - 1,
            selectedDate.dayOfMonth
        )
        //nam them
        dialog.datePicker.maxDate = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        dialog.show()
    }

    private fun updateControlState() {
        setButtonActive(btnPeriodDay, selectedPeriod == StatisticsPeriod.DAY)
        setButtonActive(btnPeriodWeek, selectedPeriod == StatisticsPeriod.WEEK)
        setButtonActive(btnPeriodMonth, selectedPeriod == StatisticsPeriod.MONTH)

        btnChooseDate.text = when (selectedPeriod) {
            StatisticsPeriod.DAY -> "Chọn ngày: ${formatDate(selectedDate)}"
            StatisticsPeriod.WEEK -> "Chọn 1 ngày trong tuần: ${formatDate(selectedDate)}"
            StatisticsPeriod.MONTH -> "Chọn 1 ngày trong tháng: ${formatDate(selectedDate)}"
        }

        val showChartControls = selectedPeriod != StatisticsPeriod.DAY
        if (showChartControls) {
            tvSelectedRangeTitle.visibility = View.VISIBLE
            tvSelectedRangeSubtitle.visibility = View.VISIBLE
            chartHint.visibility = View.VISIBLE
        }
    }

    private fun setButtonActive(button: Button, isActive: Boolean) {
        if (isActive) {
            button.setBackgroundColor(resources.getColor(R.color.wao_primary, theme))
            button.setTextColor(resources.getColor(android.R.color.white, theme))
        } else {
            button.setBackgroundColor(resources.getColor(R.color.wao_slate_300, theme))
            button.setTextColor(resources.getColor(R.color.wao_slate_600, theme))
        }
        button.isAllCaps = false
    }

    private fun metricLabel(metric: ChartMetric): String {
        return when (metric) {
            ChartMetric.WEIGHT -> "Biểu đồ cân nặng"
            ChartMetric.PROTEIN -> "Biểu đồ protein"
            ChartMetric.CARBS -> "Biểu đồ carbs"
            ChartMetric.FAT -> "Biểu đồ fat"
            ChartMetric.CALORIES -> "Biểu đồ tổng lượng calo"
        }
    }

    private fun yAxisUnitFor(metric: ChartMetric): String {
        return when (metric) {
            ChartMetric.WEIGHT -> "(kg)"
            ChartMetric.PROTEIN -> "(g)"
            ChartMetric.CARBS -> "(g)"
            ChartMetric.FAT -> "(g)"
            ChartMetric.CALORIES -> "(kcal)"
        }
    }

    private fun finishLoading() {
        progressBar.visibility = View.GONE
        tvStatus.text = "Đã cập nhật lúc ${nowTimeText()}"
    }

    private fun formatDate(date: LocalDate): String = date.format(dayFormatter)

    private fun formatRawDate(raw: String): String {
        return runCatching { formatDate(LocalDate.parse(raw)) }.getOrDefault(raw)
    }

    private fun formatCalories(value: Double?): String = "${formatNumber(value)} kcal"

    private fun formatGram(value: Double?): String = "${formatNumber(value)} g"

    private fun formatWeight(value: Double?): String {
        if (value == null) return "--"
        return "${formatNumber(value)} kg"
    }

    //nam them
    private fun formatWeightChange(value: Double?): String {
        if (value == null) return "--"
        return "${if (value > 0) "+" else ""}${formatNumber(value)} kg"
    }

    private fun formatNumber(value: Double?): String {
        if (value == null) return "--"
        return String.format(Locale.getDefault(), "%.1f", value)
    }

    private fun nowTimeText(): String =
        DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault()).format(LocalTime.now())
}
