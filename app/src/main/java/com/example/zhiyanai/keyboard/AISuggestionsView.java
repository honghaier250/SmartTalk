package com.example.zhiyanai.keyboard;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.zhiyanai.R;

import java.util.List;

/**
 * AI建议视图，用于显示AI优化后的表达建议
 */
public class AISuggestionsView extends LinearLayout {
    
    private LinearLayout suggestionsContainer;
    private OnSuggestionSelectedListener listener;
    
    public AISuggestionsView(Context context) {
        super(context);
        init(context);
    }
    
    public AISuggestionsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public AISuggestionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.ai_suggestions_view, this, true);
        
        // 获取建议容器
        suggestionsContainer = findViewById(R.id.suggestions_container);
    }
    
    /**
     * 更新AI建议列表
     * @param suggestions AI优化后的表达建议列表
     */
    public void updateSuggestions(List<String> suggestions) {
        // 清空现有建议
        suggestionsContainer.removeAllViews();
        
        // 添加新建议
        for (String suggestion : suggestions) {
            TextView suggestionView = (TextView) LayoutInflater.from(getContext())
                    .inflate(R.layout.suggestion_item, suggestionsContainer, false);
            
            suggestionView.setText(suggestion);
            
            // 设置点击事件
            suggestionView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onSuggestionSelected(suggestion);
                    }
                }
            });
            
            suggestionsContainer.addView(suggestionView);
        }
    }
    
    /**
     * 设置建议选择监听器
     * @param listener 监听器
     */
    public void setOnSuggestionSelectedListener(OnSuggestionSelectedListener listener) {
        this.listener = listener;
    }
    
    /**
     * 建议选择监听器接口
     */
    public interface OnSuggestionSelectedListener {
        void onSuggestionSelected(String suggestion);
    }
} 