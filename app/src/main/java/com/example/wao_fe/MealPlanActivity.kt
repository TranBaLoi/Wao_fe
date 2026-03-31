package com.example.wao_fe

import android.animation.Animator
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import com.airbnb.lottie.LottieAnimationView
import com.example.wao_fe.component.FloatingAddMenu
import com.example.wao_fe.component.MealPlanItemMode
import com.example.wao_fe.component.MealPlanItemRenderer
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.UserRepository
import com.example.wao_fe.network.models.ApplyMealPlanRequest
import com.example.wao_fe.network.models.MealPlanFoodResponse
import com.example.wao_fe.network.models.MealPlanResponse
import com.example.wao_fe.network.models.MealType
import com.example.wao_fe.viewmodel.MealPlanState
import com.example.wao_fe.viewmodel.MealPlanViewModel
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MealPlanActivity : AppCompatActivity() {

    private lateinit var viewModel: MealPlanViewModel
    private val userRepository = UserRepository()
    private var userId: Long = -1

    private lateinit var tvDate: TextView
    private lateinit var btnBackHeader: ImageView
    private lateinit var shimmerViewContainer: ShimmerFrameLayout
    private lateinit var contentContainer: LinearLayout
    private lateinit var emptyState: LinearLayout

    private lateinit var listBreakfast: LinearLayout
    private lateinit var listLunch: LinearLayout
    private lateinit var listDinner: LinearLayout

    private lateinit var tvBreakfastLabel: TextView
    private lateinit var tvLunchLabel: TextView
    private lateinit var tvDinnerLabel: TextView

    private lateinit var btnApply: Button
    private lateinit var bottomBar: LinearLayout
    private lateinit var fabGenerateAI: ExtendedFloatingActionButton

    private lateinit var lottieOverlay: View
    private lateinit var lottieAnimation: LottieAnimationView

    private lateinit var progressSavedPlans: ProgressBar
    private lateinit var listSavedPlans: LinearLayout
    private lateinit var tvSavedPlansEmpty: TextView

    private var savedMealPlans: MutableList<MealPlanResponse> = mutableListOf()

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var fabAddFood: FloatingActionButton
    private var floatingMenuDialog: android.app.Dialog? = null
    private var isDraftApplied = false

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents.isNullOrBlank()) {
            Toast.makeText(this, "Đã hủy quét", Toast.LENGTH_SHORT).show()
        } else {
            Log.i("BarcodeScan", "MealPlan scan: ${result.contents}")
            Toast.makeText(this, "Mã vạch: ${result.contents}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_plan)

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        userId = sharedPref.getLong("USER_ID", -1)

        viewModel = ViewModelProvider(this).get(MealPlanViewModel::class.java)

        initViews()
        setupListeners()
        observeViewModel()

        if (userId == -1L) {
            Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        if (userId != -1L) {
            loadSavedMealPlans()
        }
    }

    private fun initViews() {
        tvDate = findViewById(R.id.tvDate)
        btnBackHeader = findViewById(R.id.btnBackHeader)
        shimmerViewContainer = findViewById(R.id.shimmerViewContainer)
        contentContainer = findViewById(R.id.contentContainer)
        emptyState = findViewById(R.id.emptyState)

        listBreakfast = findViewById(R.id.listBreakfast)
        listLunch = findViewById(R.id.listLunch)
        listDinner = findViewById(R.id.listDinner)

        tvBreakfastLabel = findViewById(R.id.tvBreakfastLabel)
        tvLunchLabel = findViewById(R.id.tvLunchLabel)
        tvDinnerLabel = findViewById(R.id.tvDinnerLabel)

        btnApply = findViewById(R.id.btnApply)
        bottomBar = findViewById(R.id.bottomBar)
        fabGenerateAI = findViewById(R.id.fabGenerateAI)

        lottieOverlay = findViewById(R.id.lottieOverlay)
        lottieAnimation = findViewById(R.id.lottieAnimation)

        progressSavedPlans = findViewById(R.id.progressSavedPlans)
        listSavedPlans = findViewById(R.id.listSavedPlans)
        tvSavedPlansEmpty = findViewById(R.id.tvSavedPlansEmpty)

        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        fabAddFood = findViewById(R.id.fabAddFood)

        val sdf = SimpleDateFormat("EEEE, dd MMM", Locale("vi", "VN"))
        tvDate.text = sdf.format(Date())
    }

    private fun setupListeners() {
        btnBackHeader.setOnClickListener { finish() }

        fabGenerateAI.setOnClickListener {
            if (userId != -1L) {
                viewModel.generateMealPlan(userId)
            } else {
                Toast.makeText(this, "Không tìm thấy người dùng", Toast.LENGTH_SHORT).show()
            }
        }

        btnApply.setOnClickListener {
            if (userId == -1L) return@setOnClickListener

            if (!isDraftApplied) {
                viewModel.applySuggestionToDraft(userId)
            } else {
                confirmSaveMealPlan()
            }
        }
        fabAddFood.setOnClickListener {
            if (floatingMenuDialog?.isShowing == true) {
                floatingMenuDialog?.dismiss()
            } else {
                showFloatingMenu()
            }
        }
        bottomNavigationView.selectedItemId = R.id.nav_menu
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_diary -> {
                    startActivity(Intent(this, FoodDiaryActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, SettingsActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    finish()
                    true
                }
                R.id.nav_menu -> true
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        viewModel.state.observe(this) { state ->
            when (state) {
                is MealPlanState.Idle -> {
                    shimmerViewContainer.visibility = View.GONE
                    shimmerViewContainer.stopShimmer()
                    contentContainer.visibility = View.GONE
                    emptyState.visibility = if (savedMealPlans.isEmpty()) View.VISIBLE else View.GONE
                    bottomBar.visibility = View.GONE
                    fabGenerateAI.visibility = View.VISIBLE
                    isDraftApplied = false
                    btnApply.isEnabled = true
                    btnApply.text = "✅ Áp dụng vào bản nháp"
                }
                is MealPlanState.Loading -> {
                    emptyState.visibility = View.GONE
                    contentContainer.visibility = View.GONE
                    bottomBar.visibility = View.GONE
                    fabGenerateAI.visibility = View.GONE

                    shimmerViewContainer.visibility = View.VISIBLE
                    shimmerViewContainer.startShimmer()
                }
                is MealPlanState.SuggestionReady -> {
                    shimmerViewContainer.stopShimmer()
                    shimmerViewContainer.visibility = View.GONE
                    emptyState.visibility = View.GONE
                    contentContainer.visibility = View.VISIBLE
                    bottomBar.visibility = View.VISIBLE
                    fabGenerateAI.visibility = View.VISIBLE
                    fabGenerateAI.text = "Gợi ý lại"
                    isDraftApplied = false
                    btnApply.isEnabled = true
                    btnApply.text = "✅ Áp dụng vào bản nháp"

                    renderMealPlan(state.data.foods)
                }
                is MealPlanState.DraftReady -> {
                    shimmerViewContainer.stopShimmer()
                    shimmerViewContainer.visibility = View.GONE
                    emptyState.visibility = View.GONE
                    contentContainer.visibility = View.VISIBLE
                    bottomBar.visibility = View.VISIBLE
                    fabGenerateAI.visibility = View.VISIBLE
                    isDraftApplied = true
                    btnApply.isEnabled = true
                    btnApply.text = "💾 Lưu vào Thực đơn"

                    renderMealPlan(state.draft.previewFoods)
                    Toast.makeText(this, "Đã thêm vào Thực đơn tạm", Toast.LENGTH_SHORT).show()
                }
                is MealPlanState.SavingDraft -> {
                    btnApply.isEnabled = false
                    btnApply.text = "Đang lưu..."
                }
                is MealPlanState.DraftSaved -> {
                    btnApply.isEnabled = true
                    btnApply.text = "💾 Lưu vào Thực đơn"
                    lottieOverlay.visibility = View.VISIBLE
                    lottieAnimation.removeAllAnimatorListeners()
                    lottieAnimation.playAnimation()

                    lottieAnimation.addAnimatorListener(object : Animator.AnimatorListener {
                        override fun onAnimationStart(animation: Animator) {}
                        override fun onAnimationEnd(animation: Animator) {
                            lottieOverlay.visibility = View.GONE
                            Toast.makeText(this@MealPlanActivity, "Đã lưu Thực đơn thành công", Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                            loadSavedMealPlans()
                        }
                        override fun onAnimationCancel(animation: Animator) {}
                        override fun onAnimationRepeat(animation: Animator) {}
                    })
                }
                is MealPlanState.Error -> {
                    shimmerViewContainer.stopShimmer()
                    shimmerViewContainer.visibility = View.GONE
                    emptyState.visibility = if (savedMealPlans.isEmpty()) View.VISIBLE else View.GONE
                    fabGenerateAI.visibility = View.VISIBLE
                    btnApply.isEnabled = true
                    btnApply.text = if (isDraftApplied) "💾 Lưu vào Thực đơn" else "✅ Áp dụng vào bản nháp"
                    Toast.makeText(this, "Lỗi: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmSaveMealPlan() {
        AlertDialog.Builder(this)
            .setTitle("Lưu Thực đơn")
            .setMessage("Xác nhận lưu Thực đơn tạm lên hệ thống?")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Lưu") { _, _ ->
                viewModel.saveDraftMealPlan()
            }
            .show()
    }

    private fun renderMealPlan(foods: List<MealPlanFoodResponse>) {
        listBreakfast.removeAllViews()
        listLunch.removeAllViews()
        listDinner.removeAllViews()

        val breakfastFoods = foods.filter { it.mealType == MealType.BREAKFAST }
        val lunchFoods = foods.filter { it.mealType == MealType.LUNCH }
        val dinnerFoods = foods.filter { it.mealType == MealType.DINNER }

        // Cập nhật hiển thị label và danh sách
        toggleSection(breakfastFoods, tvBreakfastLabel, listBreakfast)
        toggleSection(lunchFoods, tvLunchLabel, listLunch)
        toggleSection(dinnerFoods, tvDinnerLabel, listDinner)
    }

    private fun toggleSection(foods: List<MealPlanFoodResponse>, label: TextView, container: LinearLayout) {
        if (foods.isEmpty()) {
            label.visibility = View.GONE
            container.visibility = View.GONE
        } else {
            label.visibility = View.VISIBLE
            container.visibility = View.VISIBLE
            for (food in foods) {
                container.addView(createFoodView(food))
            }
        }
    }

    private fun createFoodView(food: MealPlanFoodResponse): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_meal_food, null, false)

        val tvFoodName = view.findViewById<TextView>(R.id.tvFoodName)
        val tvServing = view.findViewById<TextView>(R.id.tvServing)
        val tvCalories = view.findViewById<TextView>(R.id.tvCalories)
        val tvMacros = view.findViewById<TextView>(R.id.tvMacros)
        val ivAllergenWarning = view.findViewById<ImageView>(R.id.ivAllergenWarning)

        tvFoodName.text = food.foodName ?: "Món ăn"
        tvServing.text = "${food.servingQty} phần"

        food.calories?.let {
            tvCalories.text = "${it.toInt()} kcal"
        }

        val p = food.protein?.toInt() ?: 0
        val c = food.carbs?.toInt() ?: 0
        val f = food.fat?.toInt() ?: 0
        tvMacros.text = "P: ${p}g • C: ${c}g • F: ${f}g"

        if (!food.containsAllergens.isNullOrEmpty()) {
            ivAllergenWarning.visibility = View.VISIBLE
        }

        return view
    }

    private fun showFloatingMenu() {
        if (floatingMenuDialog == null) {
            floatingMenuDialog = FloatingAddMenu.create(
                activity = this,
                onScanBarcode = { startBarcodeScanner() }
            )
            floatingMenuDialog?.setOnDismissListener {
                floatingMenuDialog = null
            }
        }
        floatingMenuDialog?.show()
    }

    private fun startBarcodeScanner() {
        val options = ScanOptions().apply {
            setPrompt("Đặt mã vạch sản phẩm vào giữa khung hình")
            setBeepEnabled(true)
            setOrientationLocked(true)
            setCaptureActivity(CustomScannerActivity::class.java)
        }
        barcodeLauncher.launch(options)
    }

    private fun loadSavedMealPlans() {
        progressSavedPlans.visibility = View.VISIBLE
        tvSavedPlansEmpty.visibility = View.GONE
        listSavedPlans.visibility = View.GONE

        lifecycleScope.launch {
            when (val result = userRepository.getUserMealPlans(userId)) {
                is ApiResult.Success -> {
                    savedMealPlans = result.data.toMutableList()
                    renderSavedMealPlans()
                }
                is ApiResult.Error -> {
                    progressSavedPlans.visibility = View.GONE
                    listSavedPlans.visibility = View.GONE
                    tvSavedPlansEmpty.visibility = View.VISIBLE
                    tvSavedPlansEmpty.text = "Không tải được danh sách Thực đơn"
                    Toast.makeText(this@MealPlanActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun renderSavedMealPlans() {
        progressSavedPlans.visibility = View.GONE
        listSavedPlans.removeAllViews()

        if (savedMealPlans.isEmpty()) {
            listSavedPlans.visibility = View.GONE
            tvSavedPlansEmpty.visibility = View.VISIBLE
            emptyState.visibility = View.VISIBLE
            return
        }

        tvSavedPlansEmpty.visibility = View.GONE
        listSavedPlans.visibility = View.VISIBLE
        if (viewModel.state.value == MealPlanState.Idle) {
            emptyState.visibility = View.GONE
        }

        MealPlanItemRenderer.render(
            container = listSavedPlans,
            mealPlans = savedMealPlans,
            mode = MealPlanItemMode.MANAGE,
            onViewDetail = { mealPlan ->
                startActivity(
                    Intent(this, MealPlanDetailActivity::class.java)
                        .putExtra(SavedMealPlansActivity.EXTRA_MEAL_PLAN_ID, mealPlan.id)
                )
            },
            onAdjust = { mealPlan ->
                showAdjustMealPlanDialog(mealPlan)
            },
            onDelete = { mealPlan ->
                confirmDeleteMealPlan(mealPlan)
            },
            onApply = { mealPlan ->
                confirmApplyMealPlanToDiary(mealPlan)
            }
        )
    }

    private fun confirmApplyMealPlanToDiary(mealPlan: MealPlanResponse) {
        AlertDialog.Builder(this)
            .setTitle("Áp dụng vào Nhật ký")
            .setMessage("Áp dụng '${mealPlan.name}' vào nhật ký ăn hôm nay?")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Áp dụng") { _, _ ->
                applyMealPlanToDiary(mealPlan)
            }
            .show()
    }

    private fun applyMealPlanToDiary(mealPlan: MealPlanResponse) {
        val logDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val request = ApplyMealPlanRequest(
            userId = userId,
            logDate = logDate
        )

        lifecycleScope.launch {
            when (val result = userRepository.applyMealPlan(mealPlan.id, request)) {
                is ApiResult.Success -> {
                    Toast.makeText(
                        this@MealPlanActivity,
                        "Đã áp dụng vào Nhật Ký cho ngày $logDate",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is ApiResult.Error -> {
                    Toast.makeText(this@MealPlanActivity, result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showAdjustMealPlanDialog(mealPlan: MealPlanResponse) {
        val spacing = (12 * resources.displayMetrics.density).toInt()
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(spacing * 2, spacing, spacing * 2, 0)
        }

        val etName = EditText(this).apply {
            hint = "Tên thực đơn"
            setText(mealPlan.name)
        }

        val etDescription = EditText(this).apply {
            hint = "Mô tả (có thể bỏ trống)"
            setText(mealPlan.description.orEmpty())
            minLines = 2
        }

        container.addView(etName)
        container.addView(etDescription)

        AlertDialog.Builder(this)
            .setTitle("Điều chỉnh thực đơn")
            .setView(container)
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Tiếp tục") { _, _ ->
                val editedName = etName.text?.toString()?.trim().orEmpty()
                if (editedName.isEmpty()) {
                    Toast.makeText(this, "Tên thực không được để trống", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val editedDescription = etDescription.text?.toString()?.trim()?.ifEmpty { null }
                viewModel.editSavedMealPlan(
                    userId = userId,
                    mealPlan = mealPlan,
                    nameOverride = editedName,
                    descriptionOverride = editedDescription
                )
                Toast.makeText(this, "Đang chỉnh sửa bản nháp", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun confirmDeleteMealPlan(mealPlan: MealPlanResponse) {
        AlertDialog.Builder(this)
            .setTitle("Xóa thực đơn")
            .setMessage("Bạn có chắc chắn muốn xóa '${mealPlan.name}'?")
            .setNegativeButton("Hủy", null)
            .setPositiveButton("Xóa") { _, _ ->
                lifecycleScope.launch {
                    when (val result = userRepository.deleteMealPlan(mealPlan.id)) {
                        is ApiResult.Success -> {
                            savedMealPlans.removeAll { it.id == mealPlan.id }
                            renderSavedMealPlans()
                            Toast.makeText(
                                this@MealPlanActivity,
                                "Đã xóa Thực đơn.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        is ApiResult.Error -> {
                            Toast.makeText(this@MealPlanActivity, result.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .show()
    }

    override fun onDestroy() {
        floatingMenuDialog?.setOnDismissListener(null)
        runCatching { floatingMenuDialog?.dismiss() }
        floatingMenuDialog = null
        super.onDestroy()
    }
}


