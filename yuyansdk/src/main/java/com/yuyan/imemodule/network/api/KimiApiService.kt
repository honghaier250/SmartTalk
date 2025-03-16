package com.yuyan.imemodule.network.api

import com.yuyan.imemodule.network.model.KimiChatRequest
import com.yuyan.imemodule.network.model.KimiChatResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

/**
 * KIMI API服务接口
 * 用于调用KIMI大模型API
 */
interface KimiApiService {
    /**
     * 调用KIMI聊天API
     * @param authorization API密钥，格式为"Bearer YOUR_API_KEY"
     * @param request 聊天请求参数
     * @return 聊天响应结果
     */
    @POST("v1/chat/completions")
    fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body request: KimiChatRequest
    ): Call<KimiChatResponse>
} 