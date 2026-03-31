package com.example.wao_fe.network

import com.example.wao_fe.network.models.ChatbotConversationDetail
import com.example.wao_fe.network.models.ChatbotConversationSummary
import com.example.wao_fe.network.models.ChatbotSendMessageRequest
import com.example.wao_fe.network.models.ChatbotSendMessageResponse

class ChatbotRepository(
    private val apiService: ApiService = NetworkClient.apiService
) {

    suspend fun getConversations(userId: Long): ApiResult<List<ChatbotConversationSummary>> {
        return safeApiCall { apiService.getChatConversations(userId) }
    }

    suspend fun getConversationDetail(
        userId: Long,
        conversationId: Long
    ): ApiResult<ChatbotConversationDetail> {
        return safeApiCall { apiService.getChatConversationDetail(userId, conversationId) }
    }

    suspend fun deleteConversation(userId: Long, conversationId: Long): ApiResult<Unit> {
        return safeApiCall { apiService.deleteChatConversation(userId, conversationId) }
    }

    suspend fun sendMessage(
        userId: Long,
        conversationId: Long?,
        message: String
    ): ApiResult<ChatbotSendMessageResponse> {
        val request = ChatbotSendMessageRequest(conversationId = conversationId, message = message)
        return safeApiCall { apiService.sendChatMessage(userId, request) }
    }

    private suspend fun <T> safeApiCall(call: suspend () -> T): ApiResult<T> {
        return try {
            ApiResult.Success(call())
        } catch (exception: IllegalArgumentException) {
            ApiResult.Error(message = exception.message ?: "Du lieu khong hop le")
        } catch (exception: Exception) {
            ApiResult.Error(message = exception.message ?: "Khong the ket noi may chu")
        }
    }
}
