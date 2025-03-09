package com.example.zhiyanai.keyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;

import com.example.zhiyanai.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 智言AI输入法服务
 * 这是输入法的核心服务类，继承自InputMethodService
 */
public class ZhiYanInputMethodService extends InputMethodService {
    
    private static final String TAG = "ZhiYanInputMethod";
    
    // 添加API密钥常量
    private static final String API_KEY = "YOUR_API_KEY";
    
    private KeyboardView keyboardView;
    private Keyboard qwertyKeyboard; // 26键键盘
    private Keyboard nineKeyboard;   // 9键键盘
    private Keyboard symbolsKeyboard; // 符号键盘
    private Keyboard numbersKeyboard; // 数字键盘
    
    private boolean isCapsLock = false;
    private boolean isChineseMode = true; // 默认中文模式
    private boolean isNineKeyMode = false; // 默认26键模式
    
    private PinyinEngine pinyinEngine; // 拼音引擎
    private CandidatesView candidatesView; // 候选词视图
    private AISuggestionsView aiSuggestionsView; // AI建议视图
    private AIService aiService; // AI服务
    
    private String currentText = ""; // 当前输入的文本
    private boolean isAIViewShown = false; // AI视图是否显示
    
    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化各种键盘
        qwertyKeyboard = new Keyboard(this, R.xml.keyboard_qwerty);
        symbolsKeyboard = new Keyboard(this, R.xml.keyboard_symbols);
        nineKeyboard = new Keyboard(this, R.xml.keyboard_nine_key);
        
        // 初始化拼音引擎
        pinyinEngine = new PinyinEngine(this);
        
        // 初始化AI服务
        aiService = new AIService(this, API_KEY);
        
        // 输出日志
        Log.d(TAG, "输入法服务已创建，拼音引擎初始化状态: " + pinyinEngine.isInitialized());
    }
    
    @Override
    public View onCreateInputView() {
        // 创建键盘视图
        keyboardView = (ZhiYanKeyboardView) getLayoutInflater().inflate(
                R.layout.keyboard_view, null);
        
        // 初始化QWERTY键盘
        qwertyKeyboard = new Keyboard(this, R.xml.keyboard_qwerty);
        
        // 设置键盘到视图
        keyboardView.setKeyboard(qwertyKeyboard);
        keyboardView.setOnKeyboardActionListener(new KeyboardView.OnKeyboardActionListener() {
            @Override
            public void onPress(int primaryCode) {
            }

            @Override
            public void onRelease(int primaryCode) {
            }

            @Override
            public void onKey(int primaryCode, int[] keyCodes) {
                handleKeyPress(primaryCode);
            }

            @Override
            public void onText(CharSequence text) {
            }

            @Override
            public void swipeLeft() {
            }

            @Override
            public void swipeRight() {
            }

            @Override
            public void swipeDown() {
            }

            @Override
            public void swipeUp() {
            }
        });
        
        return keyboardView;
    }
    
    @Override
    public View onCreateCandidatesView() {
        // 创建候选词视图
        candidatesView = new CandidatesView(this);
        candidatesView.setOnCandidateSelectedListener(new CandidatesView.OnCandidateSelectedListener() {
            @Override
            public void onCandidateSelected(String candidate) {
                // 提交选中的候选词
                commitText(candidate);
                // 清空拼音输入
                pinyinEngine.clearInput();
                // 更新候选词视图
                updateCandidatesView();
            }
        });
        return candidatesView;
    }
    
    @Override
    public void onStartInput(EditorInfo info, boolean restarting) {
        super.onStartInput(info, restarting);
        // 根据输入类型切换键盘
        int inputType = info.inputType & InputType.TYPE_MASK_CLASS;
        if (inputType == InputType.TYPE_CLASS_NUMBER || inputType == InputType.TYPE_CLASS_PHONE) {
            // 如果是数字输入，切换到数字键盘
            // 将在后续实现
        } else {
            // 默认使用字母键盘
            // 将在后续实现
        }
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 处理物理键盘按键事件
        return super.onKeyDown(keyCode, event);
    }
    
    /**
     * 处理键盘按键事件
     */
    private void handleKeyPress(int primaryCode) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;

        switch (primaryCode) {
            case -1: // Shift键
                isCapsLock = !isCapsLock;
                if (qwertyKeyboard != null) {
                    qwertyKeyboard.setShifted(isCapsLock);
                    keyboardView.invalidateAllKeys();
                }
                break;
            case -2: // 123键 - 切换到符号键盘
                keyboardView.setKeyboard(symbolsKeyboard);
                break;
            case -20000: // ABC键 - 切换回26键键盘
                keyboardView.setKeyboard(qwertyKeyboard);
                break;
            case -3: // 中/英键 - 切换中英文输入
                switchLanguageMode();
                break;
            case -4: // 完成键
                hideWindow();
                break;
            case -5: // 删除键
                if (isChineseMode && pinyinEngine.getCurrentInput().length() > 0) {
                    // 如果在中文模式下且有拼音输入，则删除拼音字符
                    pinyinEngine.deleteLastInput();
                    updateCandidatesView();
                } else {
                    // 否则删除文本
                    ic.deleteSurroundingText(1, 0);
                }
                break;
            case -6: // 分隔符键 (')
                if (isChineseMode) {
                    pinyinEngine.handleInput('\'');
                    updateCandidatesView();
                } else {
                    ic.commitText("'", 1);
                }
                break;
            case 10: // 回车键
                if (isChineseMode && pinyinEngine.getCurrentInput().length() > 0) {
                    // 如果有拼音输入，先清空
                    pinyinEngine.clearInput();
                    updateCandidatesView();
                }
                ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER));
                break;
            case 32: // 空格键
                if (isChineseMode && pinyinEngine.getCurrentInput().length() > 0) {
                    // 在中文模式下且有拼音输入时，选择第一个候选词
                    List<String> candidates = pinyinEngine.getCandidates();
                    if (candidates != null && !candidates.isEmpty()) {
                        String candidate = candidates.get(0);
                        // 如果候选词包含拼音提示（如"字(pinyin)"格式），只取汉字部分
                        if (candidate.contains("(")) {
                            candidate = candidate.substring(0, candidate.indexOf("("));
                        }
                        commitText(candidate);
                        pinyinEngine.clearInput();
                        updateCandidatesView();
                    }
                } else {
                    // 否则输入空格
                    ic.commitText(" ", 1);
                }
                break;
            case -7: // AI优化键
                // 获取当前输入框中的文本
                CharSequence text = ic.getSelectedText(0);
                if (text == null || text.length() == 0) {
                    // 如果没有选中文本，尝试获取当前段落
                    text = ic.getTextBeforeCursor(1000, 0);
                }
                
                if (text != null && text.length() > 0) {
                    currentText = text.toString();
                    optimizeExpressionWithAI(currentText);
                }
                break;
            default: // 普通字符输入
                char code = (char) primaryCode;
                if (Character.isLetter(code)) {
                    if (isCapsLock) {
                        code = Character.toUpperCase(code);
                    }
                    
                    if (isChineseMode) {
                        // 中文模式下，将字母作为拼音输入
                        pinyinEngine.handleInput(code);
                        updateCandidatesView();
                    } else {
                        // 英文模式下，直接输入字母
                        ic.commitText(String.valueOf(code), 1);
                    }
                } else {
                    // 非字母字符直接输入
                    ic.commitText(String.valueOf(code), 1);
                }
        }
    }
    
    /**
     * 更新候选词视图
     */
    private void updateCandidatesView() {
        if (candidatesView != null) {
            String pinyin = pinyinEngine.getCurrentInput();
            candidatesView.setPinyin(pinyin);
            List<String> candidates = pinyinEngine.getCandidates();
            
            Log.d(TAG, "更新候选词视图，拼音: " + pinyin + ", 候选词数量: " + candidates.size());
            
            candidatesView.updateCandidates(candidates);
            
            // 显示或隐藏候选词视图
            setCandidatesViewShown(pinyin.length() > 0);
        } else {
            Log.e(TAG, "候选词视图为空");
        }
    }
    
    /**
     * 切换中英文输入模式
     */
    private void switchLanguageMode() {
        isChineseMode = !isChineseMode;
        
        // 清空拼音输入
        if (pinyinEngine != null) {
            pinyinEngine.clearInput();
        }
        
        // 更新候选词区域
        updateCandidatesView();
        
        // 显示或隐藏候选词视图
        setCandidatesViewShown(isChineseMode && pinyinEngine.getCurrentInput().length() > 0);
        
        // 提示用户当前模式
        String modeText = isChineseMode ? "中文模式" : "英文模式";
        getCurrentInputConnection().commitText(modeText, 1);
        getCurrentInputConnection().deleteSurroundingText(modeText.length(), 0);
    }
    
    /**
     * 切换9键和26键模式
     */
    private void switchKeyboardMode() {
        isNineKeyMode = !isNineKeyMode;
        // 更新键盘布局
        keyboardView.setKeyboard(isNineKeyMode ? nineKeyboard : qwertyKeyboard);
    }
    
    /**
     * 发送文本到输入框
     */
    private void commitText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
        }
    }
    
    /**
     * 删除文本
     */
    private void deleteText() {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.deleteSurroundingText(1, 0);
        }
    }
    
    /**
     * 调用AI优化表达
     */
    private void optimizeExpressionWithAI(String text) {
        // 显示加载提示
        showAISuggestionsView();
        List<String> loadingList = new ArrayList<>();
        loadingList.add("正在生成优化建议...");
        aiSuggestionsView.updateSuggestions(loadingList);
        
        // 调用AI服务
        aiService.optimizeExpression(text, new AIService.AIResponseListener() {
            @Override
            public void onSuccess(final List<String> suggestions) {
                // 在UI线程更新建议
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (aiSuggestionsView != null) {
                            aiSuggestionsView.updateSuggestions(suggestions);
                        }
                    }
                });
            }
            
            @Override
            public void onFailure(final String errorMessage) {
                // 在UI线程显示错误
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (aiSuggestionsView != null) {
                            List<String> errorList = new ArrayList<>();
                            errorList.add("生成建议失败: " + errorMessage);
                            aiSuggestionsView.updateSuggestions(errorList);
                        }
                    }
                });
            }
        });
    }
    
    /**
     * 在UI线程运行任务
     */
    private void runOnUiThread(Runnable runnable) {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(runnable);
    }
    
    /**
     * 显示AI建议视图
     */
    private void showAISuggestionsView() {
        if (!isAIViewShown) {
            // 创建并显示AI建议视图
            View aiView = createAISuggestionsView();
            setInputView(aiView);
            isAIViewShown = true;
        }
    }
    
    /**
     * 隐藏AI建议视图
     */
    private void hideAISuggestionsView() {
        if (isAIViewShown) {
            // 恢复键盘视图
            setInputView(onCreateInputView());
            isAIViewShown = false;
        }
    }
    
    /**
     * 创建AI建议视图
     */
    public View createAISuggestionsView() {
        aiSuggestionsView = new AISuggestionsView(this);
        aiSuggestionsView.setOnSuggestionSelectedListener(new AISuggestionsView.OnSuggestionSelectedListener() {
            @Override
            public void onSuggestionSelected(String suggestion) {
                // 提交选中的AI建议
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) {
                    // 删除当前文本
                    ic.deleteSurroundingText(currentText.length(), 0);
                    // 提交新文本
                    ic.commitText(suggestion, 1);
                }
                
                // 隐藏AI建议视图
                hideAISuggestionsView();
            }
        });
        return aiSuggestionsView;
    }
}