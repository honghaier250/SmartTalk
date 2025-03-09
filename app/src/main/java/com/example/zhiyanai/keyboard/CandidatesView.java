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
 * 候选词视图
 * 用于显示拼音输入和候选词
 */
public class CandidatesView extends LinearLayout {
    
    private TextView pinyinText;
    private LinearLayout candidatesContainer;
    private OnCandidateSelectedListener listener;
    
    public CandidatesView(Context context) {
        super(context);
        init(context);
    }
    
    public CandidatesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }
    
    public CandidatesView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }
    
    private void init(Context context) {
        // 加载布局
        LayoutInflater.from(context).inflate(R.layout.candidates_view, this, true);
        
        // 获取视图引用
        pinyinText = findViewById(R.id.pinyin_text);
        candidatesContainer = findViewById(R.id.candidates_container);
    }
    
    /**
     * 设置当前输入的拼音
     */
    public void setPinyin(String pinyin) {
        if (pinyin == null || pinyin.isEmpty()) {
            pinyinText.setVisibility(View.GONE);
        } else {
            pinyinText.setVisibility(View.VISIBLE);
            pinyinText.setText(pinyin);
        }
    }
    
    /**
     * 更新候选词列表
     */
    public void updateCandidates(List<String> candidates) {
        // 清空现有候选词
        candidatesContainer.removeAllViews();
        
        // 如果没有候选词，隐藏整个视图
        if (candidates == null || candidates.isEmpty()) {
            setVisibility(View.GONE);
            return;
        }
        
        // 显示视图
        setVisibility(View.VISIBLE);
        
        // 添加候选词
        for (final String candidate : candidates) {
            TextView textView = new TextView(getContext());
            textView.setText(candidate);
            textView.setPadding(24, 16, 24, 16);
            textView.setTextSize(18);
            
            // 设置点击事件
            textView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        // 如果候选词包含拼音提示（如"字(pinyin)"格式），只取汉字部分
                        String text = candidate;
                        if (text.contains("(")) {
                            text = text.substring(0, text.indexOf("("));
                        }
                        listener.onCandidateSelected(text);
                    }
                }
            });
            
            // 添加到容器
            candidatesContainer.addView(textView);
        }
    }
    
    /**
     * 设置候选词选择监听器
     */
    public void setOnCandidateSelectedListener(OnCandidateSelectedListener listener) {
        this.listener = listener;
    }
    
    /**
     * 候选词选择监听器接口
     */
    public interface OnCandidateSelectedListener {
        void onCandidateSelected(String candidate);
    }
} 