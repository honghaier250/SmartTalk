package com.yuyan.imemodule.view.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.adapter.AIFunctionCategoryAdapter
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.entity.AIFunctionCategory
import com.yuyan.imemodule.entity.AIFunctionItem
import com.yuyan.imemodule.entity.AISubCategory
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.view.keyboard.InputView

/**
 * AI功能视图容器
 * 用于显示AI相关功能的界面
 */
@SuppressLint("ViewConstructor")
class AIContainer(context: Context, inputView: InputView) : BaseContainer(context, inputView) {
    private lateinit var mAIFunctionsView: View
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: AIFunctionCategoryAdapter
    private lateinit var mTitleView: TextView
    
    /**
     * 初始化AI功能视图
     */
    fun showAIFunctionsView() {
        if (!::mAIFunctionsView.isInitialized) {
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
                // TODO: 实现具体的AI功能处理逻辑
                // 这里可以根据不同的功能类型调用不同的处理方法
            }
            
            // 设置适配器
            mRecyclerView.adapter = mAdapter
            
            // 加载示例数据
            loadAIFunctions()
        }
        
        // 移除所有子视图并添加AI功能视图
        this.removeAllViews()
        this.addView(mAIFunctionsView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        ))
        
        // 更新输入法状态
        inputView.updateCandidateBar()
    }
    
    /**
     * 加载AI功能列表
     */
    private fun loadAIFunctions() {
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
}