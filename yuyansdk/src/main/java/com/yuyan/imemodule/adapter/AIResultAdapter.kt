package com.yuyan.imemodule.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.yuyan.imemodule.R
import com.yuyan.imemodule.data.theme.ThemeManager

/**
 * AI结果适配器
 * 用于显示AI生成的结果列表
 */
class AIResultAdapter(private val context: Context) : RecyclerView.Adapter<AIResultAdapter.ViewHolder>() {
    private val TAG = "AIResultAdapter"
    private var results: List<String> = emptyList()
    private var onItemClickListener: ((adapter: RecyclerView.Adapter<*>, view: View?, position: Int) -> Unit)? = null
    
    /**
     * 设置数据
     */
    fun setData(results: List<String>) {
        this.results = results
        notifyDataSetChanged()
    }
    
    /**
     * 获取指定位置的结果项
     */
    fun getItem(position: Int): String {
        return results[position]
    }
    
    /**
     * 设置项目点击监听器
     */
    fun setOnItemClickListener(listener: (adapter: RecyclerView.Adapter<*>, view: View?, position: Int) -> Unit) {
        this.onItemClickListener = listener
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        try {
            Log.d(TAG, "创建ViewHolder")
            val inflater = LayoutInflater.from(context)
            val view = inflater.inflate(R.layout.item_ai_result, parent, false)
            return ViewHolder(view)
        } catch (e: Exception) {
            Log.e(TAG, "创建ViewHolder失败", e)
            // 创建一个简单的备用视图
            val textView = TextView(context)
            textView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            textView.setPadding(20, 20, 20, 20)
            textView.id = R.id.tv_ai_result
            return ViewHolder(textView)
        }
    }
    
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        try {
            val result = results[position]
            holder.textView.text = result
            
            try {
                holder.textView.setTextColor(ThemeManager.activeTheme.keyTextColor.toInt())
            } catch (e: Exception) {
                Log.e(TAG, "设置文本颜色失败", e)
                // 使用默认颜色
                holder.textView.setTextColor(0xFF000000.toInt())
            }
            
            // 设置点击事件
            holder.itemView.setOnClickListener { view ->
                try {
                    onItemClickListener?.invoke(this, view, position)
                } catch (e: Exception) {
                    Log.e(TAG, "处理点击事件失败", e)
                    Toast.makeText(context, "选择失败: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "绑定ViewHolder失败", e)
            holder.textView.text = "加载失败"
        }
    }
    
    override fun getItemCount(): Int = results.size
    
    /**
     * ViewHolder类
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView by lazy {
            try {
                itemView.findViewById<TextView>(R.id.tv_ai_result)
            } catch (e: Exception) {
                Log.e(TAG, "获取TextView失败", e)
                TextView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                }
            }
        }
    }
} 