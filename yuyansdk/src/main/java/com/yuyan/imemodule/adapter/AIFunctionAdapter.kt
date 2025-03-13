package com.yuyan.imemodule.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.entity.AIFunctionItem
import com.yuyan.imemodule.data.theme.ThemeManager

/**
 * AI功能适配器
 * 用于显示AI功能列表
 */
class AIFunctionAdapter(private val context: Context) : RecyclerView.Adapter<AIFunctionAdapter.ViewHolder>() {
    private var data: List<AIFunctionItem> = ArrayList()
    private var onFunctionClickListener: ((adapter: RecyclerView.Adapter<*>, view: View?, position: Int) -> Unit)? = null

    /**
     * 设置数据
     */
    fun setData(data: List<AIFunctionItem>) {
        this.data = data
        notifyDataSetChanged()
    }

    /**
     * 获取指定位置的项目
     */
    fun getItem(position: Int): AIFunctionItem {
        return data[position]
    }

    /**
     * 设置功能项点击监听器
     */
    fun setOnFunctionClickListener(listener: (adapter: RecyclerView.Adapter<*>, view: View?, position: Int) -> Unit) {
        this.onFunctionClickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_ai_function, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = data[position]
        holder.nameTextView.text = item.name
        holder.nameTextView.setTextColor(ThemeManager.activeTheme.keyTextColor.toInt())
        holder.iconImageView.setImageResource(item.iconResId)
        holder.iconImageView.drawable?.setTint(ThemeManager.activeTheme.keyTextColor)

        // 设置点击事件
        holder.itemView.setOnClickListener { view ->
            onFunctionClickListener?.invoke(this, view, position)
        }
    }

    override fun getItemCount(): Int = data.size

    /**
     * ViewHolder类
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.tv_ai_function_name)
        val iconImageView: ImageView = itemView.findViewById(R.id.iv_ai_function_icon)
    }
}