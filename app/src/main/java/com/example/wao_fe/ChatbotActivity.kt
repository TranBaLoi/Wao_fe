package com.example.wao_fe

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wao_fe.network.ApiResult
import com.example.wao_fe.network.ChatbotRepository
import com.example.wao_fe.network.models.ChatbotMessageItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatbotActivity : AppCompatActivity() {

    private lateinit var rvMessages: RecyclerView
    private lateinit var emptyState: LinearLayout
    private lateinit var etInput: EditText
    private lateinit var btnSend: ImageView
    private lateinit var btnNewChat: ImageView
    private lateinit var btnHistory: ImageView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var rvHistory: RecyclerView
    private lateinit var drawerNewChat: LinearLayout
    private lateinit var bottomNavigationView: BottomNavigationView

    private val adapter = ChatMessageAdapter()
    private val historyAdapter = ConversationAdapter { conversationId ->
        openConversation(conversationId)
        drawerLayout.closeDrawer(GravityCompat.START)
    }
    private val repository = ChatbotRepository()

    private var userId: Long = -1L
    private var conversationId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chatbot)

        initViews()
        setupRecyclerView()
        setupHistoryRecyclerView()
        loadUserId()
        startNewConversation(showToast = false)
        loadHistoryList()
        setupSendAction()
        setupHistoryAction()
        setupNewConversationAction()
        setupBottomNavigation()
    }

    private fun initViews() {
        rvMessages = findViewById(R.id.rvChatMessages)
        emptyState = findViewById(R.id.emptyState)
        etInput = findViewById(R.id.etChatInput)
        btnSend = findViewById(R.id.btnChatSend)
        btnNewChat = findViewById(R.id.btnChatAdd)
        btnHistory = findViewById(R.id.ivChatMenu)
        drawerLayout = findViewById(R.id.drawerChat)
        rvHistory = findViewById(R.id.rvChatHistoryDrawer)
        drawerNewChat = findViewById(R.id.drawerNewChat)
        bottomNavigationView = findViewById(R.id.bottomNavigationViewChat)
    }

    private fun setupRecyclerView() {
        rvMessages.layoutManager = LinearLayoutManager(this)
        rvMessages.adapter = adapter
    }

    private fun setupHistoryRecyclerView() {
        rvHistory.layoutManager = LinearLayoutManager(this)
        rvHistory.adapter = historyAdapter
    }

    private fun loadUserId() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        userId = sharedPref.getLong("USER_ID", -1L)
        if (userId == -1L) {
            Toast.makeText(this, "Chua co thong tin nguoi dung", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadHistoryList() {
        if (userId == -1L) return
        lifecycleScope.launch {
            when (val result = repository.getConversations(userId)) {
                is ApiResult.Success -> {
                    historyAdapter.setItems(result.data)
                }
                is ApiResult.Error -> {
                    Toast.makeText(this@ChatbotActivity, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadConversationDetail(id: Long) {
        lifecycleScope.launch {
            when (val result = repository.getConversationDetail(userId, id)) {
                is ApiResult.Success -> {
                    val items = result.data.messages
                    adapter.setMessages(items)
                    showEmptyState(items.isEmpty())
                    if (items.isNotEmpty()) {
                        rvMessages.scrollToPosition(items.size - 1)
                    }
                }

                is ApiResult.Error -> {
                    showEmptyState(true)
                    Toast.makeText(this@ChatbotActivity, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupSendAction() {
        btnSend.setOnClickListener {
            val message = etInput.text.toString().trim()
            if (message.isEmpty()) return@setOnClickListener
            etInput.text?.clear()
            sendMessage(message)
        }
    }

    private fun setupHistoryAction() {
        btnHistory.setOnClickListener {
            if (userId == -1L) return@setOnClickListener
            loadHistoryList()
            drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    private fun setupNewConversationAction() {
        btnNewChat.setOnClickListener {
            startNewConversation(showToast = true)
        }
        drawerNewChat.setOnClickListener {
            startNewConversation(showToast = true)
            drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun setupBottomNavigation() {
        bottomNavigationView.selectedItemId = R.id.nav_home
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_menu -> {
                    startActivity(Intent(this, MealPlanActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, SettingsActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_diary -> {
                    startActivity(Intent(this, FoodDiaryActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NO_ANIMATION })
                    overridePendingTransition(0, 0)
                    true
                }
                else -> false
            }
        }
    }

    private fun startNewConversation(showToast: Boolean) {
        conversationId = null
        adapter.clear()
        showEmptyState(true)
        if (showToast) {
            Toast.makeText(this, "Da bat dau cuoc tro chuyen moi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openConversation(id: Long) {
        conversationId = id
        loadConversationDetail(id)
    }

    private fun sendMessage(message: String) {
        if (userId == -1L) return

        val localUser = ChatbotMessageItem(
            id = System.currentTimeMillis(),
            role = "USER",
            content = message,
            createdAt = null
        )
        adapter.addMessage(localUser)
        showEmptyState(false)
        adapter.setTypingVisible(true)
        rvMessages.scrollToPosition(adapter.itemCount - 1)
        val typingStartedAt = System.currentTimeMillis()

        lifecycleScope.launch {
            when (val result = repository.sendMessage(userId, conversationId, message)) {
                is ApiResult.Success -> {
                    keepTypingVisibleForMoment(typingStartedAt)
                    conversationId = result.data.conversationId
                    adapter.setTypingVisible(false)
                    val reply = ChatbotMessageItem(
                        id = result.data.assistantMessageId,
                        role = "ASSISTANT",
                        content = result.data.answer,
                        createdAt = result.data.createdAt
                    )
                    adapter.addMessage(reply)
                    rvMessages.scrollToPosition(adapter.itemCount - 1)
                    loadHistoryList()
                }

                is ApiResult.Error -> {
                    keepTypingVisibleForMoment(typingStartedAt)
                    adapter.setTypingVisible(false)
                    Toast.makeText(this@ChatbotActivity, result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun keepTypingVisibleForMoment(startedAt: Long) {
        val elapsed = System.currentTimeMillis() - startedAt
        val minVisibleMs = 700L
        if (elapsed < minVisibleMs) {
            delay(minVisibleMs - elapsed)
        }
    }

    private fun showEmptyState(show: Boolean) {
        emptyState.visibility = if (show) LinearLayout.VISIBLE else LinearLayout.GONE
        rvMessages.visibility = if (show) RecyclerView.GONE else RecyclerView.VISIBLE
    }

    companion object {
        const val EXTRA_CONVERSATION_ID = "conversation_id"
    }
}
