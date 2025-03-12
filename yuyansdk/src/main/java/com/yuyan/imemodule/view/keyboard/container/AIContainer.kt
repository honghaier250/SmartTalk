package com.yuyan.imemodule.view.keyboard.container

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.adapter.AIFunctionAdapter
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.entity.AIFunctionItem
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
    private lateinit var mAdapter: AIFunctionAdapter
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
            
            // 设置网格布局，每行显示3个功能按钮
            val layoutManager = GridLayoutManager(context, 3)
            mRecyclerView.layoutManager = layoutManager
            
            // 创建适配器并设置数据
            mAdapter = AIFunctionAdapter(context)
            mAdapter.setOnItemClickListener { _, _, position ->
                // 处理AI功能按钮点击事件
                val item = mAdapter.getItem(position)
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
        // 创建AI功能列表
        val functions = mutableListOf<AIFunctionItem>(
            AIFunctionItem("高情商表达", R.drawable.ic_menu_ai, 1),
            AIFunctionItem("含蓄表达", R.drawable.ic_menu_ai, 2),
            AIFunctionItem("温柔表达", R.drawable.ic_menu_ai, 3),
            AIFunctionItem("讽刺表达", R.drawable.ic_menu_ai, 4),
            AIFunctionItem("土味情话", R.drawable.ic_menu_ai, 5),
            AIFunctionItem("激励表达", R.drawable.ic_menu_ai, 6),
            AIFunctionItem("AI翻译", R.drawable.ic_menu_ai, 7),
            AIFunctionItem("职场场景", R.drawable.ic_menu_ai, 8),
            AIFunctionItem("恋爱场景", R.drawable.ic_menu_ai, 9),
            AIFunctionItem("职场精英", R.drawable.ic_menu_ai, 10),
            AIFunctionItem("纯真大学生", R.drawable.ic_menu_ai, 11),
            AIFunctionItem("智能知识库", R.drawable.ic_menu_ai, 12)
        )
        
        // 更新适配器数据
        mAdapter.setData(functions)
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