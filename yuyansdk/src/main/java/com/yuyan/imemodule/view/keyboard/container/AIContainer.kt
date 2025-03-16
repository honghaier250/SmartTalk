package com.yuyan.imemodule.view.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputConnection
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.adapter.AIFunctionCategoryAdapter
import com.yuyan.imemodule.adapter.AIResultAdapter
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.entity.AIFunctionCategory
import com.yuyan.imemodule.entity.AIFunctionItem
import com.yuyan.imemodule.entity.AISubCategory
import com.yuyan.imemodule.network.KimiApiClient
import com.yuyan.imemodule.network.model.KimiResult
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.service.DecodingInfo
import com.yuyan.imemodule.service.ImeService
import com.yuyan.imemodule.utils.thread.ThreadPoolUtils
import com.yuyan.imemodule.view.keyboard.InputView
import com.yuyan.inputmethod.core.CandidateListItem
import com.yuyan.inputmethod.core.Kernel

/**
 * AI功能视图容器
 * 用于显示AI相关功能的界面
 */
@SuppressLint("ViewConstructor")
class AIContainer(context: Context, inputView: InputView) : BaseContainer(context, inputView) {
    private val TAG = "AIContainer"
    private lateinit var mAIFunctionsView: View
    private lateinit var mAIResultsView: View
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mResultsRecyclerView: RecyclerView
    private lateinit var mAdapter: AIFunctionCategoryAdapter
    private lateinit var mResultsAdapter: AIResultAdapter
    private lateinit var mTitleView: TextView
    private lateinit var mResultsTitleView: TextView
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mErrorMessageView: TextView
    
    // 保存ImeService实例
    private var imeService: ImeService? = null
    
    // 主线程Handler，用于UI更新
    private val mainHandler = Handler(Looper.getMainLooper())
    
    // KIMI API客户端
    private val mKimiApiClient = KimiApiClient.getInstance()
    
    // 测试用的默认文本，当无法获取输入文本时使用
    private val defaultTestText = "这是一个测试文本，用于演示AI功能。实际使用时，请先在输入框中输入文字，再点击AI功能按钮。"
    
    init {
        // 尝试获取ImeService实例
        imeService = inputView.getTag(R.id.ime_service_tag) as? ImeService
        
        // 设置KIMI API密钥（实际应用中应从安全存储中获取）
        mKimiApiClient.setApiKey("sk-FL1MNDnGyTuV361N5Bdo7xkF1ucpBq9cUNqmORoAMRuBHHi7") // 替换为实际的API密钥
    }
    
    /**
     * 初始化AI功能视图
     */
    fun showAIFunctionsView() {
        Log.d(TAG, "showAIFunctionsView: 初始化AI功能视图")
        if (!::mAIFunctionsView.isInitialized) {
            try {
            // 加载AI功能视图布局
            mAIFunctionsView = LayoutInflater.from(context).inflate(R.layout.layout_ime_ai_functions, this, false)
            
            // 初始化标题和RecyclerView
            mTitleView = mAIFunctionsView.findViewById(R.id.tv_ai_functions_title)
            mTitleView.setTextColor(ThemeManager.activeTheme.keyTextColor.toInt())
            mRecyclerView = mAIFunctionsView.findViewById(R.id.rv_ai_functions)
            
            // 设置线性布局，用于显示分类列表
            val layoutManager = LinearLayoutManager(context)
            mRecyclerView.layoutManager = layoutManager
            
            // 创建适配器并设置数据
            mAdapter = AIFunctionCategoryAdapter(context)
            mAdapter.setOnFunctionClickListener { _, _, categoryPosition, functionPosition ->
                // 处理AI功能按钮点击事件
                val category = mAdapter.getCategories()[categoryPosition]
                val function = category.functions[functionPosition]
                    
                    Log.d(TAG, "点击了功能: ${function.name}, 类型: ${category.categoryType}")
                    
                    // 获取当前输入框中的文本
                    val inputText = getCurrentInputText()
                    Log.d(TAG, "获取到的输入文本: $inputText")
                    
                    // 根据不同的分类和功能类型处理
                    when (category.categoryType) {
                        // 基础表达维度
                        1 -> {
                            if (inputText.isNotEmpty()) {
                                // 根据功能类型获取表达风格
                                val expressionType = when (function.functionType) {
                                    1 -> "正式/商务体"
                                    2 -> "口语化/朋友体"
                                    3 -> "高情商沟通体"
                                    else -> "标准体"
                                }
                                
                                // 显示AI结果视图
                                showAIResultsView(expressionType)
                                
                                // 显示加载中
                                showLoading(true)
                                
                                // 显示Toast提示
                                Toast.makeText(context, "正在生成${expressionType}表达...", Toast.LENGTH_SHORT).show()
                                
                                Log.d(TAG, "调用KIMI API优化表达, 类型: $expressionType, 文本: $inputText")
                                
                                // 调用KIMI API优化表达
                                mKimiApiClient.optimizeExpression(
                                    inputText = inputText, 
                                    expressionType = expressionType, 
                                    callback = { result ->
                                        // 在主线程中更新UI
                                        mainHandler.post {
                                            if (result.success) {
                                                Log.d(TAG, "API调用成功, 结果: ${result.data}")
                                                // 显示优化结果
                                                showResults(result.data)
                                            } else {
                                                Log.e(TAG, "API调用失败: ${result.errorMessage}")
                                                // 显示错误信息
                                                showError(result.errorMessage ?: "未知错误")
                                                // 显示Toast提示
                                                Toast.makeText(context, "生成失败: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            } else {
                                Log.e(TAG, "输入文本为空，使用默认测试文本")
                                // 使用默认测试文本
                                val defaultText = defaultTestText
                                
                                // 根据功能类型获取表达风格
                                val expressionType = when (function.functionType) {
                                    1 -> "正式/商务体"
                                    2 -> "口语化/朋友体"
                                    3 -> "高情商沟通体"
                                    else -> "标准体"
                                }
                                
                                // 显示AI结果视图
                                showAIResultsView(expressionType)
                                
                                // 显示加载中
                                showLoading(true)
                                
                                // 显示Toast提示
                                Toast.makeText(context, "未检测到输入文本，使用默认文本生成${expressionType}表达...\n实际使用时，请先输入文字再点击AI功能", Toast.LENGTH_LONG).show()
                                
                                // 为多语混合体准备自定义提示词
                                val customSystemPrompt = if (function.functionType == 9) {
                                    "你是一个专业的中英混合表达助手，请根据用户的输入，生成5种不同的中英夹杂风格的表达方式。" +
                                    "要求：1. 保持原文的主要意思；2. 将部分关键词或短语替换为英文表达；3. 确保中英混合自然流畅，符合中国人日常使用的中英混合表达习惯；" +
                                    "4. 不要完全翻译成英文，而是生成中英混合的表达；5. 英文部分约占30%-50%，以达到时尚国际化的效果。" +
                                    "请直接给出优化后的5个表达，每个表达一行，不要有编号，不要有任何解释和额外文字。"
                                } else null
                                
                                val customUserMessage = if (function.functionType == 9) {
                                    "请将以下中文文本优化为自然流畅的中英混合表达，保持原意但使用中英夹杂的表达方式：$defaultText"
                                } else null
                                
                                // 调用KIMI API优化表达
                                mKimiApiClient.optimizeExpression(
                                    inputText = defaultText, 
                                    expressionType = expressionType, 
                                    callback = { result ->
                                        // 在主线程中更新UI
                                        mainHandler.post {
                                            if (result.success) {
                                                Log.d(TAG, "API调用成功, 结果: ${result.data}")
                                                // 显示优化结果
                                                showResults(result.data)
                                            } else {
                                                Log.e(TAG, "API调用失败: ${result.errorMessage}")
                                                // 显示错误信息
                                                showError(result.errorMessage ?: "未知错误")
                                                // 显示Toast提示
                                                Toast.makeText(context, "生成失败: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    customSystemPrompt = customSystemPrompt,
                                    customUserMessage = customUserMessage
                                )
                            }
                        }
                        
                        // 文化风格维度
                        2 -> {
                            if (inputText.isNotEmpty()) {
                                // 根据功能类型获取表达风格
                                val expressionType = when (function.functionType) {
                                    4 -> "古风雅言体"
                                    5 -> "方言趣味体"
                                    6 -> "二次元破壁体"
                                    else -> "标准体"
                                }
                                
                                // 显示AI结果视图
                                showAIResultsView(expressionType)
                                
                                // 显示加载中
                                showLoading(true)
                                
                                // 显示Toast提示
                                Toast.makeText(context, "正在生成${expressionType}表达...", Toast.LENGTH_SHORT).show()
                                
                                Log.d(TAG, "调用KIMI API优化表达, 类型: $expressionType, 文本: $inputText")
                                
                                // 调用KIMI API优化表达
                                mKimiApiClient.optimizeExpression(
                                    inputText = inputText, 
                                    expressionType = expressionType, 
                                    callback = { result ->
                                        // 在主线程中更新UI
                                        mainHandler.post {
                                            if (result.success) {
                                                Log.d(TAG, "API调用成功, 结果: ${result.data}")
                                                // 显示优化结果
                                                showResults(result.data)
                                            } else {
                                                Log.e(TAG, "API调用失败: ${result.errorMessage}")
                                                // 显示错误信息
                                                showError(result.errorMessage ?: "未知错误")
                                                // 显示Toast提示
                                                Toast.makeText(context, "生成失败: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            } else {
                                Log.e(TAG, "输入文本为空，使用默认测试文本")
                                // 使用默认测试文本
                                val defaultText = defaultTestText
                                
                                // 根据功能类型获取表达风格
                                val expressionType = when (function.functionType) {
                                    4 -> "古风雅言体"
                                    5 -> "方言趣味体"
                                    6 -> "二次元破壁体"
                                    else -> "标准体"
                                }
                                
                                // 显示AI结果视图
                                showAIResultsView(expressionType)
                                
                                // 显示加载中
                                showLoading(true)
                                
                                // 显示Toast提示
                                Toast.makeText(context, "未检测到输入文本，使用默认文本生成${expressionType}表达...\n实际使用时，请先输入文字再点击AI功能", Toast.LENGTH_LONG).show()
                                
                                // 调用KIMI API优化表达
                                mKimiApiClient.optimizeExpression(
                                    inputText = defaultText, 
                                    expressionType = expressionType, 
                                    callback = { result ->
                                        // 在主线程中更新UI
                                        mainHandler.post {
                                            if (result.success) {
                                                Log.d(TAG, "API调用成功, 结果: ${result.data}")
                                                // 显示优化结果
                                                showResults(result.data)
                                            } else {
                                                Log.e(TAG, "API调用失败: ${result.errorMessage}")
                                                // 显示错误信息
                                                showError(result.errorMessage ?: "未知错误")
                                                // 显示Toast提示
                                                Toast.makeText(context, "生成失败: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        
                        // 功能强化维度
                        3 -> {
                            if (inputText.isNotEmpty()) {
                                // 根据功能类型获取表达风格
                                val expressionType = when (function.functionType) {
                                    7 -> "极简高效体"
                                    8 -> "说服力强化体"
                                    9 -> "多语混合体"
                                    else -> "标准体"
                                }
                                
                                // 显示AI结果视图
                                showAIResultsView(expressionType)
                                
                                // 显示加载中
                                showLoading(true)
                                
                                // 显示Toast提示
                                Toast.makeText(context, "正在生成${expressionType}表达...", Toast.LENGTH_SHORT).show()
                                
                                Log.d(TAG, "调用KIMI API优化表达, 类型: $expressionType, 文本: $inputText")
                                
                                // 为多语混合体准备自定义提示词
                                val customSystemPrompt = if (function.functionType == 9) {
                                    "你是一个专业的中英混合表达助手，请根据用户的输入，生成5种不同的中英夹杂风格的表达方式。" +
                                    "要求：1. 保持原文的主要意思；2. 将部分关键词或短语替换为英文表达；3. 确保中英混合自然流畅，符合中国人日常使用的中英混合表达习惯；" +
                                    "4. 不要完全翻译成英文，而是生成中英混合的表达；5. 英文部分约占30%-50%，以达到时尚国际化的效果。" +
                                    "请直接给出优化后的5个表达，每个表达一行，不要有编号，不要有任何解释和额外文字。"
                                } else null
                                
                                val customUserMessage = if (function.functionType == 9) {
                                    "请将以下中文文本优化为自然流畅的中英混合表达，保持原意但使用中英夹杂的表达方式：$inputText"
                                } else null
                                
                                // 调用KIMI API优化表达
                                mKimiApiClient.optimizeExpression(
                                    inputText = inputText, 
                                    expressionType = expressionType, 
                                    callback = { result ->
                                        // 在主线程中更新UI
                                        mainHandler.post {
                                            if (result.success) {
                                                Log.d(TAG, "API调用成功, 结果: ${result.data}")
                                                // 显示优化结果
                                                showResults(result.data)
                                            } else {
                                                Log.e(TAG, "API调用失败: ${result.errorMessage}")
                                                // 显示错误信息
                                                showError(result.errorMessage ?: "未知错误")
                                                // 显示Toast提示
                                                Toast.makeText(context, "生成失败: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    customSystemPrompt = customSystemPrompt,
                                    customUserMessage = customUserMessage
                                )
                            } else {
                                Log.e(TAG, "输入文本为空，使用默认测试文本")
                                // 使用默认测试文本
                                val defaultText = defaultTestText
                                
                                // 根据功能类型获取表达风格
                                val expressionType = when (function.functionType) {
                                    7 -> "极简高效体"
                                    8 -> "说服力强化体"
                                    9 -> "多语混合体"
                                    else -> "标准体"
                                }
                                
                                // 显示AI结果视图
                                showAIResultsView(expressionType)
                                
                                // 显示加载中
                                showLoading(true)
                                
                                // 显示Toast提示
                                Toast.makeText(context, "未检测到输入文本，使用默认文本生成${expressionType}表达...\n实际使用时，请先输入文字再点击AI功能", Toast.LENGTH_LONG).show()
                                
                                // 为多语混合体准备自定义提示词
                                val customSystemPrompt = if (function.functionType == 9) {
                                    "你是一个专业的中英混合表达助手，请根据用户的输入，生成5种不同的中英夹杂风格的表达方式。" +
                                    "要求：1. 保持原文的主要意思；2. 将部分关键词或短语替换为英文表达；3. 确保中英混合自然流畅，符合中国人日常使用的中英混合表达习惯；" +
                                    "4. 不要完全翻译成英文，而是生成中英混合的表达；5. 英文部分约占30%-50%，以达到时尚国际化的效果。" +
                                    "请直接给出优化后的5个表达，每个表达一行，不要有编号，不要有任何解释和额外文字。"
                                } else null
                                
                                val customUserMessage = if (function.functionType == 9) {
                                    "请将以下中文文本优化为自然流畅的中英混合表达，保持原意但使用中英夹杂的表达方式：$defaultText"
                                } else null
                                
                                // 调用KIMI API优化表达
                                mKimiApiClient.optimizeExpression(
                                    inputText = defaultText, 
                                    expressionType = expressionType, 
                                    callback = { result ->
                                        // 在主线程中更新UI
                                        mainHandler.post {
                                            if (result.success) {
                                                Log.d(TAG, "API调用成功, 结果: ${result.data}")
                                                // 显示优化结果
                                                showResults(result.data)
                                            } else {
                                                Log.e(TAG, "API调用失败: ${result.errorMessage}")
                                                // 显示错误信息
                                                showError(result.errorMessage ?: "未知错误")
                                                // 显示Toast提示
                                                Toast.makeText(context, "生成失败: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    customSystemPrompt = customSystemPrompt,
                                    customUserMessage = customUserMessage
                                )
                            }
                        }
                        
                        // 情感表达维度
                        4 -> {
                            if (inputText.isNotEmpty()) {
                                // 根据功能类型获取表达风格
                                val expressionType = when (function.functionType) {
                                    10 -> "非暴力沟通体"
                                    11 -> "情感放大器"
                                    else -> "标准体"
                                }
                                
                                // 显示AI结果视图
                                showAIResultsView(expressionType)
                                
                                // 显示加载中
                                showLoading(true)
                                
                                // 显示Toast提示
                                Toast.makeText(context, "正在生成${expressionType}表达...", Toast.LENGTH_SHORT).show()
                                
                                Log.d(TAG, "调用KIMI API优化表达, 类型: $expressionType, 文本: $inputText")
                                
                                // 调用KIMI API优化表达
                                mKimiApiClient.optimizeExpression(
                                    inputText = inputText, 
                                    expressionType = expressionType, 
                                    callback = { result ->
                                        // 在主线程中更新UI
                                        mainHandler.post {
                                            if (result.success) {
                                                Log.d(TAG, "API调用成功, 结果: ${result.data}")
                                                // 显示优化结果
                                                showResults(result.data)
                                            } else {
                                                Log.e(TAG, "API调用失败: ${result.errorMessage}")
                                                // 显示错误信息
                                                showError(result.errorMessage ?: "未知错误")
                                                // 显示Toast提示
                                                Toast.makeText(context, "生成失败: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            } else {
                                Log.e(TAG, "输入文本为空，使用默认测试文本")
                                // 使用默认测试文本
                                val defaultText = defaultTestText
                                
                                // 根据功能类型获取表达风格
                                val expressionType = when (function.functionType) {
                                    10 -> "非暴力沟通体"
                                    11 -> "情感放大器"
                                    else -> "标准体"
                                }
                                
                                // 显示AI结果视图
                                showAIResultsView(expressionType)
                                
                                // 显示加载中
                                showLoading(true)
                                
                                // 显示Toast提示
                                Toast.makeText(context, "未检测到输入文本，使用默认文本生成${expressionType}表达...\n实际使用时，请先输入文字再点击AI功能", Toast.LENGTH_LONG).show()
                                
                                // 调用KIMI API优化表达
                                mKimiApiClient.optimizeExpression(
                                    inputText = defaultText, 
                                    expressionType = expressionType, 
                                    callback = { result ->
                                        // 在主线程中更新UI
                                        mainHandler.post {
                                            if (result.success) {
                                                Log.d(TAG, "API调用成功, 结果: ${result.data}")
                                                // 显示优化结果
                                                showResults(result.data)
                                            } else {
                                                Log.e(TAG, "API调用失败: ${result.errorMessage}")
                                                // 显示错误信息
                                                showError(result.errorMessage ?: "未知错误")
                                                // 显示Toast提示
                                                Toast.makeText(context, "生成失败: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        
                        // 智能翻译维度
                        5 -> {
                            if (inputText.isNotEmpty()) {
                                // 根据功能类型获取翻译语言
                                val targetLanguage = when (function.functionType) {
                                    12 -> "英语"
                                    13 -> "日语"
                                    14 -> "韩语"
                                    15 -> "法语"
                                    16 -> "德语"
                                    else -> "英语"
                                }
                                
                                // 显示AI结果视图
                                showAIResultsView("${targetLanguage}翻译")
                                
                                // 显示加载中
                                showLoading(true)
                                
                                // 显示Toast提示
                                Toast.makeText(context, "正在翻译为${targetLanguage}...", Toast.LENGTH_SHORT).show()
                                
                                Log.d(TAG, "调用KIMI API翻译, 目标语言: $targetLanguage, 文本: $inputText")
                                
                                // 调用KIMI API翻译
                                translateText(inputText, targetLanguage) { result ->
                                    // 在主线程中更新UI
                                    mainHandler.post {
                                        if (result.success) {
                                            Log.d(TAG, "API调用成功, 结果: ${result.data}")
                                            // 显示翻译结果
                                            showResults(result.data)
                                        } else {
                                            Log.e(TAG, "API调用失败: ${result.errorMessage}")
                                            // 显示错误信息
                                            showError(result.errorMessage ?: "未知错误")
                                            // 显示Toast提示
                                            Toast.makeText(context, "翻译失败: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            } else {
                                Log.e(TAG, "输入文本为空，使用默认测试文本")
                                // 使用默认测试文本
                                val defaultText = defaultTestText
                                
                                // 根据功能类型获取翻译语言
                                val targetLanguage = when (function.functionType) {
                                    12 -> "英语"
                                    13 -> "日语"
                                    14 -> "韩语"
                                    15 -> "法语"
                                    16 -> "德语"
                                    else -> "英语"
                                }
                                
                                // 显示AI结果视图
                                showAIResultsView("${targetLanguage}翻译")
                                
                                // 显示加载中
                                showLoading(true)
                                
                                // 显示Toast提示
                                Toast.makeText(context, "未检测到输入文本，使用默认文本翻译为${targetLanguage}...\n实际使用时，请先输入文字再点击AI功能", Toast.LENGTH_LONG).show()
                                
                                // 调用KIMI API翻译
                                translateText(defaultText, targetLanguage) { result ->
                                    // 在主线程中更新UI
                                    mainHandler.post {
                                        if (result.success) {
                                            Log.d(TAG, "API调用成功, 结果: ${result.data}")
                                            // 显示翻译结果
                                            showResults(result.data)
                                        } else {
                                            Log.e(TAG, "API调用失败: ${result.errorMessage}")
                                            // 显示错误信息
                                            showError(result.errorMessage ?: "未知错误")
                                            // 显示Toast提示
                                            Toast.makeText(context, "翻译失败: ${result.errorMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        }
                    }
            }
            
            // 设置适配器
            mRecyclerView.adapter = mAdapter
            
            // 加载示例数据
            loadAIFunctions()
            } catch (e: Exception) {
                Log.e(TAG, "初始化AI功能视图失败", e)
                Toast.makeText(context, "初始化AI功能视图失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
        
        try {
        // 移除所有子视图并添加AI功能视图
        this.removeAllViews()
        this.addView(mAIFunctionsView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ))
        
        // 更新输入法状态
        inputView.updateCandidateBar()
        } catch (e: Exception) {
            Log.e(TAG, "显示AI功能视图失败", e)
            Toast.makeText(context, "显示AI功能视图失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 获取当前输入框中的文本
     * 使用InputMethodService框架获取用户已经输入的文本
     */
    private fun getCurrentInputText(): String {
        try {
            Log.d(TAG, "开始尝试获取输入文本")
            
            // 优先使用保存的ImeService实例
            if (imeService != null) {
                // 首先尝试使用ImeService的getCurrentInputText方法
                val serviceText = imeService?.getCurrentInputText()
                if (serviceText != null && serviceText.isNotEmpty()) {
                    Log.d(TAG, "从保存的ImeService.getCurrentInputText获取到文本: [$serviceText]")
                    return serviceText
                }
                
                // 获取当前编辑框中的文本
                val ic = imeService?.currentInputConnection
                if (ic != null) {
                    // 获取当前文本的选择范围
                    val selection = ic.getSelectedText(0)
                    if (selection != null && selection.isNotEmpty()) {
                        Log.d(TAG, "从选中文本获取到: [$selection]")
                        return selection.toString()
                    }
                    
                    // 获取光标前的所有文本（尝试获取更多文本，最多2000个字符）
                    val beforeText = ic.getTextBeforeCursor(2000, 0)?.toString() ?: ""
                    // 获取光标后的所有文本
                    val afterText = ic.getTextAfterCursor(2000, 0)?.toString() ?: ""
                    
                    val fullText = beforeText + afterText
                    Log.d(TAG, "从InputConnection获取到文本: 光标前[$beforeText], 光标后[$afterText], 完整[$fullText]")
                    
                    if (fullText.isNotEmpty()) {
                        return fullText
                    }
                    
                    // 尝试获取整个文本字段的内容
                    val extractedText = ic.getExtractedText(android.view.inputmethod.ExtractedTextRequest(), 0)
                    if (extractedText != null && extractedText.text != null) {
                        val text = extractedText.text.toString()
                        Log.d(TAG, "从ExtractedText获取到文本: [$text]")
                        if (text.isNotEmpty()) {
                            return text
                        }
                    }
                }
                
                // 尝试从service获取上次提交的文本
                val lastCommittedText = imeService?.lastCommittedText
                if (lastCommittedText != null && lastCommittedText.isNotEmpty()) {
                    Log.d(TAG, "从保存的ImeService.lastCommittedText获取到文本: [$lastCommittedText]")
                    return lastCommittedText
                }
            } else {
                Log.e(TAG, "保存的ImeService实例为空")
            }
            
            // 尝试从context获取ImeService
            val service = context.applicationContext.getSystemService("inputmethod") as? ImeService
            if (service != null) {
                // 首先尝试使用ImeService的getCurrentInputText方法
                val serviceText = service.getCurrentInputText()
                if (serviceText != null && serviceText.isNotEmpty()) {
                    Log.d(TAG, "从系统服务获取的ImeService.getCurrentInputText获取到文本: [$serviceText]")
                    return serviceText
                }
                
                // 尝试从service获取上次提交的文本
                val lastCommittedText = service.lastCommittedText
                if (lastCommittedText != null && lastCommittedText.isNotEmpty()) {
                    Log.d(TAG, "从系统服务获取的ImeService.lastCommittedText获取到文本: [$lastCommittedText]")
                    return lastCommittedText
                }
            } else {
                Log.e(TAG, "无法从系统服务获取ImeService实例")
            }
            
            // 尝试从Kernel获取已提交的文本
            val committedText = Kernel.commitText
            if (committedText.isNotEmpty()) {
                Log.d(TAG, "从Kernel.commitText获取到文本: [$committedText]")
                return committedText
            }
            
            // 尝试从DecodingInfo获取已提交的文本
            val composingText = DecodingInfo.composingStrForCommit
            if (composingText.isNotEmpty()) {
                Log.d(TAG, "从DecodingInfo.composingStrForCommit获取到文本: [$composingText]")
                return composingText
            }
            
            // 尝试从DecodingInfo获取候选词
            try {
                val candidates = DecodingInfo.candidates
                if (candidates.isNotEmpty()) {
                    val candidateText = candidates.joinToString(" ") { it.text }
                    Log.d(TAG, "从DecodingInfo.candidates获取到文本: [$candidateText]")
                    if (candidateText.isNotEmpty()) {
                        return candidateText
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "获取候选词失败", e)
            }
            
            // 如果所有方法都失败，返回空字符串
            Log.e(TAG, "无法获取输入文本，所有方法都失败")
            return ""
        } catch (e: Exception) {
            Log.e(TAG, "获取输入文本时发生异常", e)
            return ""
        }
    }
    
    /**
     * 显示AI结果视图
     */
    private fun showAIResultsView(expressionType: String) {
        Log.d(TAG, "showAIResultsView: 显示AI结果视图, 类型: $expressionType")
        try {
            // 清空候选词列表，确保候选词栏不显示AI结果
            DecodingInfo.reset()
            
            // 如果已经初始化过，先清除之前的状态
            if (::mAIResultsView.isInitialized) {
                try {
                    // 重置视图状态
                    mProgressBar.visibility = View.GONE
                    mErrorMessageView.visibility = View.GONE
                    mResultsRecyclerView.visibility = View.VISIBLE
                    
                    // 清空适配器数据
                    if (::mResultsAdapter.isInitialized) {
                        mResultsAdapter.setData(emptyList())
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "重置AI结果视图状态失败", e)
                    // 如果重置失败，重新初始化视图
                    mAIResultsView = LayoutInflater.from(context).inflate(R.layout.layout_ai_results, this, false)
                }
            }
            
            if (!::mAIResultsView.isInitialized) {
                // 加载AI结果视图布局
                try {
                    mAIResultsView = LayoutInflater.from(context).inflate(R.layout.layout_ai_results, this, false)
                    
                    // 初始化视图组件
                    mResultsTitleView = mAIResultsView.findViewById(R.id.tv_ai_results_title)
                    mResultsRecyclerView = mAIResultsView.findViewById(R.id.rv_ai_results)
                    mProgressBar = mAIResultsView.findViewById(R.id.progress_bar)
                    mErrorMessageView = mAIResultsView.findViewById(R.id.tv_error_message)
                    
                    // 设置关闭按钮点击事件
                    val closeButton = mAIResultsView.findViewById<View>(R.id.btn_close_results)
                    closeButton.setOnClickListener {
                        Log.d(TAG, "关闭按钮被点击，返回AI功能视图")
                        showAIFunctionsView()
                    }
                    
                    // 设置标题颜色
                    try {
                        mResultsTitleView.setTextColor(ThemeManager.activeTheme.keyTextColor.toInt())
                    } catch (e: Exception) {
                        Log.e(TAG, "设置标题颜色失败", e)
                    }
                    
                    // 设置RecyclerView
                    val layoutManager = LinearLayoutManager(context)
                    mResultsRecyclerView.layoutManager = layoutManager
                    
                    // 创建适配器
                    mResultsAdapter = AIResultAdapter(context)
                    mResultsAdapter.setOnItemClickListener { _, _, position ->
                        // 处理结果项点击事件
                        if (mResultsAdapter.itemCount > position) {
                            try {
                                val selectedText = mResultsAdapter.getItem(position)
                                Log.d(TAG, "选择了结果: $selectedText")
                                
                                // 将选中的文本填入输入框
                                replaceInputText(selectedText)
                                
                                // 显示Toast提示
                                Toast.makeText(context, "已选择: $selectedText", Toast.LENGTH_SHORT).show()
                                
                                // 切换回输入键盘
                                inputView.onSettingsMenuClick(SkbMenuMode.AI)
                            } catch (e: Exception) {
                                Log.e(TAG, "选择结果失败", e)
                                Toast.makeText(context, "选择结果失败: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    
                    // 设置适配器
                    mResultsRecyclerView.adapter = mResultsAdapter
                } catch (e: Exception) {
                    Log.e(TAG, "初始化AI结果视图失败", e)
                    Toast.makeText(context, "初始化AI结果视图失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    
                    // 创建一个简单的备用视图
                    val linearLayout = LinearLayout(context)
                    linearLayout.orientation = LinearLayout.VERTICAL
                    linearLayout.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT
                    )
                    
                    val textView = TextView(context)
                    textView.text = "加载失败，请重试"
                    textView.gravity = android.view.Gravity.CENTER
                    textView.layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        setMargins(20, 20, 20, 20)
                    }
                    
                    linearLayout.addView(textView)
                    mAIResultsView = linearLayout
                    
                    // 创建一个空的适配器
                    mResultsAdapter = AIResultAdapter(context)
                    
                    // 返回到AI功能视图
                    mainHandler.postDelayed({
                        showAIFunctionsView()
                    }, 2000)
                    
                    return
                }
            } else {
                // 如果已经初始化过，确保关闭按钮的点击事件已设置
                try {
                    val closeButton = mAIResultsView.findViewById<View>(R.id.btn_close_results)
                    closeButton.setOnClickListener {
                        Log.d(TAG, "关闭按钮被点击，返回AI功能视图")
                        showAIFunctionsView()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "设置关闭按钮点击事件失败", e)
                }
            }
            
            try {
                // 设置标题
                if (::mResultsTitleView.isInitialized) {
                    mResultsTitleView.text = "$expressionType 优化结果"
                }
                
                // 移除所有子视图并添加AI结果视图
                this.removeAllViews()
                this.addView(mAIResultsView, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
                ))
            } catch (e: Exception) {
                Log.e(TAG, "添加AI结果视图失败", e)
                Toast.makeText(context, "添加AI结果视图失败: ${e.message}", Toast.LENGTH_SHORT).show()
                
                // 返回到AI功能视图
                mainHandler.postDelayed({
                    showAIFunctionsView()
                }, 2000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "显示AI结果视图失败", e)
            Toast.makeText(context, "显示AI结果视图失败: ${e.message}", Toast.LENGTH_SHORT).show()
            
            // 返回到AI功能视图
            mainHandler.postDelayed({
                showAIFunctionsView()
            }, 2000)
        }
    }
    
    /**
     * 替换输入框中的文本
     */
    private fun replaceInputText(newText: String) {
        try {
            Log.d(TAG, "开始替换输入文本为: $newText")
            
            // 优先使用保存的ImeService实例
            val ic = imeService?.currentInputConnection ?: (inputView.context.getSystemService("inputmethod") as? ImeService)?.currentInputConnection
            
            if (ic != null) {
                // 使用更可靠的方法清除文本
                // 1. 先获取当前文本长度
                val beforeText = ic.getTextBeforeCursor(10000, 0)
                val afterText = ic.getTextAfterCursor(10000, 0)
                val beforeLength = beforeText?.length ?: 0
                val afterLength = afterText?.length ?: 0
                
                Log.d(TAG, "当前文本长度: 光标前=$beforeLength, 光标后=$afterLength")
                
                // 2. 开始批量编辑
                ic.beginBatchEdit()
                
                // 3. 清除所有文本
                if (beforeLength > 0) {
                    ic.deleteSurroundingText(beforeLength, 0)
                }
                if (afterLength > 0) {
                    ic.deleteSurroundingText(0, afterLength)
                }
                
                // 4. 插入新文本
                ic.commitText(newText, 1)
                
                // 5. 结束批量编辑
                ic.endBatchEdit()
                
                // 更新最后提交的文本
                imeService?.updateLastCommittedText(newText)
                
                Log.d(TAG, "成功替换输入文本: $newText")
            } else {
                // 如果无法获取InputConnection，使用DecodingInfo和chooseAndUpdate方法
                Log.d(TAG, "无法获取InputConnection，使用DecodingInfo和chooseAndUpdate方法")
                try {
                    // 先尝试清除现有文本
                    val service = inputView.context as? ImeService
                    if (service != null) {
                        val connection = service.currentInputConnection
                        if (connection != null) {
                            connection.beginBatchEdit()
                            connection.deleteSurroundingText(10000, 10000)
                            connection.endBatchEdit()
                        }
                    }
                    
                    // 然后使用候选词方式插入新文本
                    DecodingInfo.cacheCandidates(arrayOf(CandidateListItem("", newText)))
                    inputView.chooseAndUpdate(0)
                    Log.d(TAG, "使用DecodingInfo方法替换文本成功")
                } catch (e: Exception) {
                    Log.e(TAG, "DecodingInfo方法失败", e)
                    throw e
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "替换输入文本失败", e)
            // 尝试最后的备用方法
            try {
                // 尝试直接使用InputView的commitText方法
                val method = InputView::class.java.getDeclaredMethod("commitText", String::class.java)
                method.isAccessible = true
                method.invoke(inputView, newText)
                Log.d(TAG, "使用反射调用commitText方法替换文本成功")
            } catch (e2: Exception) {
                Log.e(TAG, "所有替换文本方法都失败", e2)
                Toast.makeText(context, "替换文本失败: ${e2.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 显示加载中状态
     */
    private fun showLoading(isLoading: Boolean) {
        Log.d(TAG, "showLoading: $isLoading")
        try {
            // 检查所有必要的视图组件是否已初始化
            if (!::mProgressBar.isInitialized || !::mResultsRecyclerView.isInitialized || !::mErrorMessageView.isInitialized) {
                Log.e(TAG, "showLoading: 视图组件未初始化")
                
                // 如果AI结果视图已初始化，尝试重新获取视图组件
                if (::mAIResultsView.isInitialized) {
                    try {
                        mProgressBar = mAIResultsView.findViewById(R.id.progress_bar)
                        mResultsRecyclerView = mAIResultsView.findViewById(R.id.rv_ai_results)
                        mErrorMessageView = mAIResultsView.findViewById(R.id.tv_error_message)
                    } catch (e: Exception) {
                        Log.e(TAG, "重新获取视图组件失败", e)
                        return
                    }
                } else {
                    return
                }
            }
            
            // 更新视图状态
            try {
                mProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                mResultsRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
                mErrorMessageView.visibility = View.GONE
            } catch (e: Exception) {
                Log.e(TAG, "更新视图状态失败", e)
                
                // 如果更新失败，尝试使用Toast显示加载状态
                if (isLoading) {
                    Toast.makeText(context, "正在加载...", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "显示加载状态失败", e)
            
            // 如果显示失败，尝试使用Toast显示加载状态
            if (isLoading) {
                Toast.makeText(context, "正在加载...", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    /**
     * 显示结果列表
     */
    private fun showResults(results: List<String>) {
        Log.d(TAG, "showResults: 显示结果列表, 数量: ${results.size}")
        try {
            // 检查适配器是否已初始化
            if (!::mResultsAdapter.isInitialized) {
                Log.e(TAG, "showResults: 结果适配器未初始化")
                
                // 如果AI结果视图已初始化，尝试重新创建适配器
                if (::mAIResultsView.isInitialized && ::mResultsRecyclerView.isInitialized) {
                    try {
                        mResultsAdapter = AIResultAdapter(context)
                        mResultsAdapter.setOnItemClickListener { _, _, position ->
                            // 处理结果项点击事件
                            if (mResultsAdapter.itemCount > position) {
                                try {
                                    val selectedText = mResultsAdapter.getItem(position)
                                    Log.d(TAG, "选择了结果: $selectedText")
                                    
                                    // 将选中的文本填入输入框
                                    replaceInputText(selectedText)
                                    
                                    // 显示Toast提示
                                    Toast.makeText(context, "已选择: $selectedText", Toast.LENGTH_SHORT).show()
                                    
                                    // 切换回输入键盘
                                    inputView.onSettingsMenuClick(SkbMenuMode.AI)
                                } catch (e: Exception) {
                                    Log.e(TAG, "选择结果失败", e)
                                    Toast.makeText(context, "选择结果失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        mResultsRecyclerView.adapter = mResultsAdapter
                    } catch (e: Exception) {
                        Log.e(TAG, "重新创建适配器失败", e)
                        Toast.makeText(context, "加载结果失败: ${e.message}", Toast.LENGTH_SHORT).show()
                        return
                    }
                } else {
                    Toast.makeText(context, "加载结果失败: 视图未初始化", Toast.LENGTH_SHORT).show()
                    return
                }
            }
            
            // 更新适配器数据
            try {
                mResultsAdapter.setData(results)
                
                // 隐藏加载中状态
                showLoading(false)
                
                // 显示成功提示
                Toast.makeText(context, "已生成${results.size}条优化表达", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "更新适配器数据失败", e)
                Toast.makeText(context, "显示结果失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "显示结果列表失败", e)
            Toast.makeText(context, "显示结果列表失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 显示错误信息
     */
    private fun showError(errorMessage: String) {
        Log.e(TAG, "showError: $errorMessage")
        try {
            // 检查所有必要的视图组件是否已初始化
            if (!::mErrorMessageView.isInitialized || !::mProgressBar.isInitialized || !::mResultsRecyclerView.isInitialized) {
                Log.e(TAG, "showError: 视图组件未初始化")
                
                // 如果AI结果视图已初始化，尝试重新获取视图组件
                if (::mAIResultsView.isInitialized) {
                    try {
                        mErrorMessageView = mAIResultsView.findViewById(R.id.tv_error_message)
                        mProgressBar = mAIResultsView.findViewById(R.id.progress_bar)
                        mResultsRecyclerView = mAIResultsView.findViewById(R.id.rv_ai_results)
                    } catch (e: Exception) {
                        Log.e(TAG, "重新获取视图组件失败", e)
                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                        return
                    }
                } else {
                    Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                    return
                }
            }
            
            // 更新视图状态
            try {
                mErrorMessageView.text = errorMessage
                mErrorMessageView.visibility = View.VISIBLE
                mProgressBar.visibility = View.GONE
                mResultsRecyclerView.visibility = View.GONE
            } catch (e: Exception) {
                Log.e(TAG, "更新视图状态失败", e)
                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "显示错误信息失败", e)
            Toast.makeText(context, "显示错误信息失败: ${e.message}", Toast.LENGTH_SHORT).show()
            Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 加载AI功能列表
     */
    private fun loadAIFunctions() {
        Log.d(TAG, "loadAIFunctions: 加载AI功能列表")
        try {
        // 创建四个维度的分类列表
        val categories = listOf(
            // 基础表达维度
            AIFunctionCategory(
                name = "基础表达维度",
                iconResId = R.drawable.ic_menu_basic_expression,
                categoryType = 1,
                functions = listOf(
                    AIFunctionItem("正式/商务体", R.drawable.ic_menu_ai, 1),
                    AIFunctionItem("口语化/朋友体", R.drawable.ic_menu_ai, 2),
                    AIFunctionItem("高情商沟通体", R.drawable.ic_menu_ai, 3)
                )
            ),
            // 文化风格维度
            AIFunctionCategory(
                name = "文化风格维度",
                iconResId = R.drawable.ic_menu_cultural_style,
                categoryType = 2,
                functions = listOf(
                    AIFunctionItem("古风雅言体", R.drawable.ic_menu_ai, 4),
                    AIFunctionItem("方言趣味体", R.drawable.ic_menu_ai, 5),
                    AIFunctionItem("二次元破壁体", R.drawable.ic_menu_ai, 6)
                )
            ),
            // 功能强化维度
            AIFunctionCategory(
                name = "功能强化维度",
                iconResId = R.drawable.ic_menu_function_enhance,
                categoryType = 3,
                functions = listOf(
                    AIFunctionItem("极简高效体", R.drawable.ic_menu_ai, 7),
                    AIFunctionItem("说服力强化体", R.drawable.ic_menu_ai, 8),
                    AIFunctionItem("多语混合体", R.drawable.ic_menu_ai, 9)
                )
            ),
            // 情感表达维度
            AIFunctionCategory(
                name = "情感表达维度",
                iconResId = R.drawable.ic_menu_emotion_expression,
                categoryType = 4,
                functions = listOf(
                    AIFunctionItem("非暴力沟通体", R.drawable.ic_menu_ai, 10),
                    AIFunctionItem("情感放大器", R.drawable.ic_menu_ai, 11)
                )
            ),
            // 智能翻译维度
            AIFunctionCategory(
                name = "智能翻译维度",
                iconResId = R.drawable.ic_menu_translate,
                categoryType = 5,
                functions = listOf(
                    AIFunctionItem("英语翻译", R.drawable.ic_menu_ai, 12),
                    AIFunctionItem("日语翻译", R.drawable.ic_menu_ai, 13),
                    AIFunctionItem("韩语翻译", R.drawable.ic_menu_ai, 14),
                    AIFunctionItem("法语翻译", R.drawable.ic_menu_ai, 15),
                    AIFunctionItem("德语翻译", R.drawable.ic_menu_ai, 16)
                )
            )
        )
        
        // 更新适配器数据
        mAdapter.setData(categories)
        } catch (e: Exception) {
            Log.e(TAG, "加载AI功能列表失败", e)
            Toast.makeText(context, "加载AI功能列表失败: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * 获取当前菜单模式
     */
    fun getMenuMode(): SkbMenuMode {
        return SkbMenuMode.AI
    }
    
    /**
     * 更新键盘布局
     */
    override fun updateSkbLayout() {
        showAIFunctionsView()
    }
    
    /**
     * 翻译文本
     * @param inputText 要翻译的文本
     * @param targetLanguage 目标语言
     * @param callback 回调函数，返回翻译结果
     */
    private fun translateText(inputText: String, targetLanguage: String, callback: (KimiResult) -> Unit) {
        Log.d(TAG, "开始翻译文本为$targetLanguage: $inputText")
        
        try {
            // 构建系统提示词
            val systemPrompt = "你是一个专业的翻译助手，请将用户的输入准确翻译成${targetLanguage}。" +
                    "请直接给出5种不同的翻译结果，每个翻译一行，不要有编号，不要有任何解释和额外文字。" +
                    "确保翻译准确表达原文的意思，符合${targetLanguage}的语言习惯。"
            
            // 构建用户消息
            val userMessage = "请将以下文本翻译成${targetLanguage}：$inputText"
            
            Log.d(TAG, "系统提示词: $systemPrompt")
            Log.d(TAG, "用户消息: $userMessage")
            
            // 调用KIMI API进行翻译
            mKimiApiClient.optimizeExpression(
                inputText = inputText, 
                expressionType = "${targetLanguage}翻译", 
                callback = callback, 
                customSystemPrompt = systemPrompt, 
                customUserMessage = userMessage
            )
        } catch (e: Exception) {
            Log.e(TAG, "翻译过程中发生异常", e)
            callback(KimiResult(false, errorMessage = "翻译过程中发生异常: ${e.message}"))
        }
    }
}