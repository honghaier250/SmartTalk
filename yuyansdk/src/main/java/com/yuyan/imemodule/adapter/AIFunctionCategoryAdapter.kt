package com.yuyan.imemodule.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.entity.AIFunctionCategory

/**
 * AI功能分类适配器
 * 用于显示AI功能分类列表，支持展开/收起功能
 */
class AIFunctionCategoryAdapter(private val context: Context) : RecyclerView.Adapter<AIFunctionCategoryAdapter.ViewHolder>() {
    private var categories: List<AIFunctionCategory> = ArrayList()
    private var onFunctionClickListener: ((adapter: RecyclerView.Adapter<*>, view: View?, categoryPosition: Int, functionPosition: Int) -> Unit)? = null

    /**
     * 设置数据
     */
    fun setData(categories: List<AIFunctionCategory>) {
        this.categories = categories
        notifyDataSetChanged()
    }

    /**
     * 设置功能项点击监听器
     */
    fun setOnFunctionClickListener(listener: (adapter: RecyclerView.Adapter<*>, view: View?, categoryPosition: Int, functionPosition: Int) -> Unit) {
        this.onFunctionClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_ai_function_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        
        // 设置分类标题和图标
        holder.nameTextView.text = category.name
        holder.nameTextView.setTextColor(ThemeManager.activeTheme.keyTextColor.toInt())
        holder.iconImageView.setImageResource(category.iconResId)
        holder.iconImageView.drawable?.setTint(ThemeManager.activeTheme.keyTextColor)
        
        // 设置展开/收起指示器
        holder.expandIndicator.rotation = if (category.isExpanded) 180f else 0f
        holder.expandIndicator.drawable?.setTint(ThemeManager.activeTheme.keyTextColor)
        
        // 设置功能列表
        holder.functionsRecyclerView.visibility = if (category.isExpanded) View.VISIBLE else View.GONE
        
        // 设置功能列表适配器
        if (category.functions.isNotEmpty()) {
            // 确保每次都重新设置布局和适配器，以便正确显示当前分类的功能项
            val layoutManager = GridLayoutManager(context, 4)
            holder.functionsRecyclerView.layoutManager = layoutManager
            
            // 创建新的适配器并设置当前分类的功能项
            val adapter = AIFunctionAdapter(context)
            adapter.setData(category.functions)
            adapter.setOnFunctionClickListener { _, view, functionPosition ->
                onFunctionClickListener?.invoke(this, view, position, functionPosition)
            }
            holder.functionsRecyclerView.adapter = adapter
        }
        
        // 设置分类头部点击事件
        holder.categoryHeader.setOnClickListener {
            category.isExpanded = !category.isExpanded
            notifyItemChanged(position)
        }
    }

    /**
     * 获取分类列表
     */
    fun getCategories(): List<AIFunctionCategory> {
        return categories
    }

    override fun getItemCount(): Int = categories.size

    /**
     * ViewHolder类
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryHeader: LinearLayout = itemView.findViewById(R.id.ll_category_header)
        val nameTextView: TextView = itemView.findViewById(R.id.tv_category_name)
        val iconImageView: ImageView = itemView.findViewById(R.id.iv_category_icon)
        val expandIndicator: ImageView = itemView.findViewById(R.id.iv_expand_indicator)
        val functionsRecyclerView: RecyclerView = itemView.findViewById(R.id.rv_functions)
    }
}