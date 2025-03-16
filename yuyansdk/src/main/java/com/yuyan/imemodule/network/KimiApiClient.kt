package com.yuyan.imemodule.network

import android.util.Log
import com.yuyan.imemodule.network.api.KimiApiService
import com.yuyan.imemodule.network.model.KimiChatRequest
import com.yuyan.imemodule.network.model.KimiChatResponse
import com.yuyan.imemodule.network.model.KimiMessage
import com.yuyan.imemodule.network.model.KimiResult
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * KIMI API客户端
 * 用于调用KIMI大模型API
 */
class KimiApiClient private constructor() {
    private val TAG = "KimiApiClient"
    
    // API基础URL
    private val BASE_URL = "https://api.moonshot.cn/"
    
    // API密钥，实际应用中应从安全存储中获取
    private var apiKey: String = "YOUR_API_KEY"
    
    // Retrofit服务
    private val service: KimiApiService
    
    init {
        Log.d(TAG, "初始化KIMI API客户端")
        // 创建OkHttpClient，设置超时和日志
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
        
        // 创建Retrofit实例
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        
        // 创建API服务
        service = retrofit.create(KimiApiService::class.java)
    }
    
    /**
     * 设置API密钥
     */
    fun setApiKey(key: String) {
        Log.d(TAG, "设置API密钥: ${key.take(5)}...")
        this.apiKey = key
    }
    
    /**
     * 调用KIMI API优化文本表达
     * @param inputText 用户输入的文本
     * @param expressionType 表达类型（如"正式/商务体"、"口语化/朋友体"等）
     * @param callback 回调函数，返回优化后的文本列表
     * @param customSystemPrompt 自定义系统提示词，如果为null则使用默认提示词
     * @param customUserMessage 自定义用户消息，如果为null则使用默认消息
     */
    fun optimizeExpression(
        inputText: String,
        expressionType: String,
        callback: (KimiResult) -> Unit,
        customSystemPrompt: String? = null,
        customUserMessage: String? = null
    ) {
        Log.d(TAG, "开始优化表达, 类型: $expressionType, 文本: $inputText")
        
        // 检查输入文本是否为空
        if (inputText.isBlank()) {
            Log.e(TAG, "输入文本为空")
            callback(KimiResult(false, errorMessage = "输入文本不能为空"))
            return
        }
        
        try {
            // 清理输入文本，去除多余的空白字符
            val cleanedText = inputText.trim()
            Log.d(TAG, "清理后的输入文本: $cleanedText")
            
            // 构建系统提示词
            val systemPrompt = customSystemPrompt ?: "你是一个专业的文本优化助手，请根据用户的输入，生成5种不同的'$expressionType'风格的表达方式。" +
                    "请直接给出优化后的5个表达，每个表达一行，不要有编号，不要有任何解释和额外文字。" +
                    "确保优化后的表达保持原意，但更符合'$expressionType'的风格。" +
                    "请注意：不要生成与用户输入无关的内容，必须基于用户的原始输入进行优化。"
            
            // 构建用户消息
            val userMessage = customUserMessage ?: "请优化以下文本，保持原意但使用'$expressionType'风格：$cleanedText"
            
            Log.d(TAG, "系统提示词: $systemPrompt")
            Log.d(TAG, "用户消息: $userMessage")
            
            // 构建请求
            val request = KimiChatRequest(
                model = "moonshot-v1-8k", // 使用KIMI的基础模型
                messages = listOf(
                    KimiMessage(role = "system", content = systemPrompt),
                    KimiMessage(role = "user", content = userMessage)
                ),
                temperature = 0.7f, // 适当的创造性
                stream = false // 非流式响应
            )
            
            Log.d(TAG, "发送API请求...")
            
            // 发送请求
            val call = service.chatCompletions("Bearer $apiKey", request)
            call.enqueue(object : Callback<KimiChatResponse> {
                override fun onResponse(call: Call<KimiChatResponse>, response: Response<KimiChatResponse>) {
                    if (response.isSuccessful) {
                        val kimiResponse = response.body()
                        Log.d(TAG, "API响应成功: $kimiResponse")
                        
                        if (kimiResponse != null && kimiResponse.choices.isNotEmpty()) {
                            // 获取助手的回复
                            val assistantMessage = kimiResponse.choices[0].message.content
                            Log.d(TAG, "助手回复: $assistantMessage")
                            
                            // 解析回复，提取5个优化表达
                            val expressions = assistantMessage.trim().split("\n")
                                .filter { it.isNotBlank() }
                                .take(5) // 确保最多只取5个
                            
                            // 检查是否获取到了表达
                            if (expressions.isEmpty()) {
                                Log.e(TAG, "未能提取到有效表达")
                                callback(KimiResult(false, errorMessage = "未能生成有效的优化表达"))
                                return
                            }
                            
                            Log.d(TAG, "提取的表达: $expressions")
                            callback(KimiResult(true, data = expressions))
                        } else {
                            Log.e(TAG, "API响应为空")
                            callback(KimiResult(false, errorMessage = "响应为空"))
                        }
                    } else {
                        val errorBody = response.errorBody()?.string() ?: "未知错误"
                        Log.e(TAG, "API调用失败: $errorBody, 状态码: ${response.code()}")
                        callback(KimiResult(false, errorMessage = "API调用失败: $errorBody, 状态码: ${response.code()}"))
                    }
                }
                
                override fun onFailure(call: Call<KimiChatResponse>, t: Throwable) {
                    Log.e(TAG, "网络请求失败", t)
                    callback(KimiResult(false, errorMessage = "网络请求失败: ${t.message}"))
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "优化表达过程中发生异常", e)
            callback(KimiResult(false, errorMessage = "优化表达过程中发生异常: ${e.message}"))
        }
    }
    
    companion object {
        @Volatile
        private var instance: KimiApiClient? = null
        
        fun getInstance(): KimiApiClient {
            return instance ?: synchronized(this) {
                instance ?: KimiApiClient().also { instance = it }
            }
        }
    }
} 