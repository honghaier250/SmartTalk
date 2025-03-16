package com.yuyan.imemodule.network.model

import com.google.gson.annotations.SerializedName

/**
 * KIMI聊天请求模型
 */
data class KimiChatRequest(
    /**
     * 模型名称，例如"moonshot-v1-8k"
     */
    val model: String,
    /**
     * 消息列表
     */
    val messages: List<KimiMessage>,
    /**
     * 温度参数，控制生成文本的随机性，值越大随机性越强
     */
    val temperature: Float = 0.7f,
    /**
     * 是否流式响应
     */
    val stream: Boolean = false
)

/**
 * KIMI消息模型
 */
data class KimiMessage(
    /**
     * 消息角色，可以是"system"、"user"或"assistant"
     */
    val role: String,
    /**
     * 消息内容
     */
    val content: String
)

/**
 * KIMI聊天响应模型
 */
data class KimiChatResponse(
    /**
     * 响应ID
     */
    val id: String,
    /**
     * 对象类型
     */
    val `object`: String,
    /**
     * 创建时间戳
     */
    val created: Long,
    /**
     * 模型名称
     */
    val model: String,
    /**
     * 响应消息列表
     */
    val choices: List<KimiChoice>,
    /**
     * 使用情况统计
     */
    val usage: KimiUsage
)

/**
 * KIMI选择模型
 */
data class KimiChoice(
    /**
     * 选择索引
     */
    val index: Int,
    /**
     * 消息内容
     */
    val message: KimiMessage,
    /**
     * 结束原因
     */
    @SerializedName("finish_reason")
    val finishReason: String
)

/**
 * KIMI使用情况统计
 */
data class KimiUsage(
    /**
     * 提示令牌数
     */
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    /**
     * 完成令牌数
     */
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    /**
     * 总令牌数
     */
    @SerializedName("total_tokens")
    val totalTokens: Int
)

/**
 * KIMI API响应结果
 */
data class KimiResult(
    /**
     * 是否成功
     */
    val success: Boolean,
    /**
     * 错误信息
     */
    val errorMessage: String? = null,
    /**
     * 响应数据
     */
    val data: List<String> = emptyList()
) 