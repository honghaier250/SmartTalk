package com.example.zhiyanai.keyboard;

import android.content.Context;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.util.AttributeSet;
import android.util.Log;

/**
 * 自定义键盘视图
 * 扩展KeyboardView以添加自定义功能和样式
 */
public class ZhiYanKeyboardView extends KeyboardView {
    
    private static final String TAG = "ZhiYanKeyboardView";
    
    public ZhiYanKeyboardView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ZhiYanKeyboardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }
    
    /**
     * 设置键盘按键的预览弹窗
     */
    @Override
    public void onDraw(android.graphics.Canvas canvas) {
        super.onDraw(canvas);
        // 自定义键盘绘制
        setPreviewEnabled(true); // 启用按键预览
        setPadding(0, 5, 0, 5); // 设置内边距
    }
    
    /**
     * 自定义按键的外观
     */
    @Override
    public boolean onLongPress(Keyboard.Key key) {
        // 处理长按事件，例如显示特殊字符
        return super.onLongPress(key);
    }
}