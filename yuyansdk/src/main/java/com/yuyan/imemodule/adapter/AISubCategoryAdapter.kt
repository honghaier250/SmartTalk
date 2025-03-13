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
import com.yuyan.imemodule.entity.AISubCategory
import com.yuyan.imemodule.adapter.AIFunctionAdapter

/**
 * AI功能子分类适配器
 * 用于显示AI功能子分类列表，支持展开/收起功能
 */
class AISubCategoryAdapter(private val context: Context) : RecyclerView.Adapter<AISubCategoryAdapter.ViewHolder>() {
    private var subCategories: List<AISubCategory> = ArrayList()
    private var onFunctionClickListener: ((adapter: RecyclerView.Adapter<*>, view: View?, subCategoryPosition: Int, functionPosition: Int) -> Unit)? = null

    /**
     * 设置数据
     */
    fun setData(subCategories: List<AISubCategory>) {
        this.subCategories = subCategories
        notifyDataSetChanged()
    }

    /**
     * 设置功能项点击监听器
     */
    fun setOnFunctionClickListener(listener: (adapter: RecyclerView.Adapter<*>, view: View?, subCategoryPosition: Int, functionPosition: Int) -> Unit) {
        this.onFunctionClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_ai_subcategory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val subCategory = subCategories[position]
        
        // 设置子分类标题
        holder.nameTextView.text = subCategory.name
        holder.nameTextView.setTextColor(ThemeManager.activeTheme.keyTextColor.toInt())
        
        // 设置展开/收起指示器
        holder.expandIndicator.rotation = if (subCategory.isExpanded) 180f else 0f
        holder.expandIndicator.drawable?.setTint(ThemeManager.activeTheme.keyTextColor)
        
        // 设置功能列表
        holder.functionsRecyclerView.visibility = if (subCategory.isExpanded) View.VISIBLE else View.GONE
        
        // 设置功能列表适配器
        if (holder.functionsRecyclerView.adapter == null) {
            val layoutManager = GridLayoutManager(context, 3) // 设置为3列网格布局
            holder.functionsRecyclerView.layoutManager = layoutManager
            
            val adapter = AIFunctionAdapter(context)
            adapter.setData(subCategory.functions)
            adapter.setOnFunctionClickListener { _, view, functionPosition ->
                onFunctionClickListener?.invoke(this, view, position, functionPosition)
            }
            holder.functionsRecyclerView.adapter = adapter
        }
        
        // 设置子分类头部点击事件
        holder.subCategoryHeader.setOnClickListener {
            subCategory.isExpanded = !subCategory.isExpanded
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = subCategories.size

    /**
     * ViewHolder类
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val subCategoryHeader: LinearLayout = itemView.findViewById(R.id.ll_subcategory_header)
        val nameTextView: TextView = itemView.findViewById(R.id.tv_subcategory_name)
        val expandIndicator: ImageView = itemView.findViewById(R.id.iv_subcategory_expand)
        val functionsRecyclerView: RecyclerView = itemView.findViewById(R.id.rv_subcategory_functions)
    }
}