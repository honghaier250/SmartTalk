package com.yuyan.imemodule.view.keyboard

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.SystemClock
import android.text.InputType
import android.view.InputDevice
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.get
import com.yuyan.imemodule.R
import com.yuyan.imemodule.callback.CandidateViewListener
import com.yuyan.imemodule.callback.IResponseKeyEvent
import com.yuyan.imemodule.data.emojicon.EmojiconData.SymbolPreset
import com.yuyan.imemodule.data.emojicon.YuyanEmojiCompat
import com.yuyan.imemodule.data.theme.ThemeManager
import com.yuyan.imemodule.database.DataBaseKT
import com.yuyan.imemodule.entity.keyboard.SoftKey
import com.yuyan.imemodule.manager.InputModeSwitcherManager
import com.yuyan.imemodule.prefs.AppPrefs.Companion.getInstance
import com.yuyan.imemodule.prefs.behavior.KeyboardOneHandedMod
import com.yuyan.imemodule.prefs.behavior.PopupMenuMode
import com.yuyan.imemodule.prefs.behavior.SkbMenuMode
import com.yuyan.imemodule.service.DecodingInfo
import com.yuyan.imemodule.service.ImeService
import com.yuyan.imemodule.singleton.EnvironmentSingleton
import com.yuyan.imemodule.ui.utils.InputMethodUtil
import com.yuyan.imemodule.utils.DevicesUtils
import com.yuyan.imemodule.utils.KeyboardLoaderUtil
import com.yuyan.imemodule.utils.StringUtils
import com.yuyan.imemodule.view.CandidatesBar
import com.yuyan.imemodule.view.ComposingView
import com.yuyan.imemodule.view.EditPhrasesView
import com.yuyan.imemodule.view.FullDisplayKeyboardBar
import com.yuyan.imemodule.view.keyboard.container.AIContainer
import com.yuyan.imemodule.view.keyboard.container.CandidatesContainer
import com.yuyan.imemodule.view.keyboard.container.ClipBoardContainer
import com.yuyan.imemodule.view.keyboard.container.SymbolContainer
import com.yuyan.imemodule.view.keyboard.container.T9TextContainer
import com.yuyan.imemodule.view.popup.PopupComponent
import com.yuyan.imemodule.view.preference.ManagedPreference
import com.yuyan.imemodule.view.widget.LifecycleRelativeLayout
import com.yuyan.inputmethod.CustomEngine
import com.yuyan.inputmethod.core.CandidateListItem
import splitties.bitflags.hasFlag
import splitties.views.bottomPadding
import splitties.views.rightPadding
import kotlin.math.absoluteValue

/**
 * 输入法主界面。
 * 包含拼音显示、候选词栏、键盘界面等。
 * 在 该类中处理界面绘制、、输入逻辑等为输入法核心处理类。
 * 注: 所有键盘自定义 View禁用构造方法警告，且不创建含AttributeSet的构造方法。为了实现代码混淆效果。
 */

@SuppressLint("ViewConstructor")
class InputView(context: Context, service: ImeService) : LifecycleRelativeLayout(context), IResponseKeyEvent {
    private val clipboardItemTimeout = getInstance().clipboard.clipboardItemTimeout.getValue()
    private var chinesePrediction = true
    var isAddPhrases = false
    private var service: ImeService
    private var mImeState = ImeState.STATE_IDLE // 当前的输入法状态
    private var mChoiceNotifier = ChoiceNotifier()
    private var mComposingView: ComposingView // 组成字符串的View，用于显示输入的拼音。
    var mSkbRoot: RelativeLayout
    var mSkbCandidatesBarView: CandidatesBar //候选词栏根View
    private var mHoderLayoutLeft: LinearLayout
    private var mHoderLayoutRight: LinearLayout
    private lateinit var mOnehandHoderLayout: LinearLayout
    var mAddPhrasesLayout: EditPhrasesView
    private var mLlKeyboardBottomHolder: LinearLayout
    private lateinit var mRightPaddingKey: ManagedPreference.PInt
    private lateinit var mBottomPaddingKey: ManagedPreference.PInt
    private var mFullDisplayKeyboardBar:FullDisplayKeyboardBar? = null

    init {
        initNavbarBackground(service)
        this.service = service
        mSkbRoot = LayoutInflater.from(context).inflate(R.layout.sdk_skb_container, this, false) as RelativeLayout
        addView(mSkbRoot)
        mSkbCandidatesBarView = mSkbRoot.findViewById(R.id.candidates_bar)
        mHoderLayoutLeft = mSkbRoot.findViewById(R.id.ll_skb_holder_layout_left)
        mHoderLayoutRight = mSkbRoot.findViewById(R.id.ll_skb_holder_layout_right)
        mAddPhrasesLayout = EditPhrasesView(context)
        KeyboardManager.instance.setData(mSkbRoot.findViewById(R.id.skb_input_keyboard_view), this)
        mLlKeyboardBottomHolder =  mSkbRoot.findViewById(R.id.iv_keyboard_holder)
        mComposingView = ComposingView(context)
        addView(mComposingView,  LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            addRule(ABOVE, mSkbRoot.id)
            addRule(ALIGN_LEFT, mSkbRoot.id)
        })
        val root = PopupComponent.get().root
        val viewParent = root.parent
        if (viewParent != null) {
            (viewParent as ViewGroup).removeView(root)
        }
        addView(root, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
            addRule(ALIGN_BOTTOM, mSkbRoot.id)
            addRule(ALIGN_LEFT, mSkbRoot.id)
        })
        initView(context)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun initView(context: Context) {
        if(isAddPhrases){
            if(mAddPhrasesLayout.parent == null) {
                addView(mAddPhrasesLayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    addRule(ABOVE, mSkbRoot.id)
                    addRule(ALIGN_LEFT, mSkbRoot.id)
                })
                mAddPhrasesLayout.handleAddPhrasesView()
            }
        } else {
            removeView(mAddPhrasesLayout)
        }
        mSkbCandidatesBarView.initialize(mChoiceNotifier)
        val oneHandedModSwitch = getInstance().keyboardSetting.oneHandedModSwitch.getValue()
        val oneHandedMod = getInstance().keyboardSetting.oneHandedMod.getValue()
        if(::mOnehandHoderLayout.isInitialized)mOnehandHoderLayout.visibility = GONE
        if (oneHandedModSwitch) {
            mOnehandHoderLayout = when(oneHandedMod){
                KeyboardOneHandedMod.LEFT ->  mHoderLayoutRight
                else -> mHoderLayoutLeft
            }
            mOnehandHoderLayout.visibility = VISIBLE
            mOnehandHoderLayout[0].setOnClickListener { view: View -> onClick(view) }
            mOnehandHoderLayout[1].setOnClickListener { view: View -> onClick(view) }
            (mOnehandHoderLayout[1] as ImageButton).setImageResource(if (oneHandedMod == KeyboardOneHandedMod.LEFT) R.drawable.ic_menu_one_hand_right else R.drawable.ic_menu_one_hand)
            val layoutParamsHoder = mOnehandHoderLayout.layoutParams
            layoutParamsHoder.width = EnvironmentSingleton.instance.holderWidth
            layoutParamsHoder.height = EnvironmentSingleton.instance.skbHeight
        }
        mLlKeyboardBottomHolder.removeAllViews()
        mLlKeyboardBottomHolder.layoutParams.width = EnvironmentSingleton.instance.skbWidth
        if(EnvironmentSingleton.instance.keyboardModeFloat){
            mBottomPaddingKey = (if(EnvironmentSingleton.instance.isLandscape) getInstance().internal.keyboardBottomPaddingLandscapeFloat
                else getInstance().internal.keyboardBottomPaddingFloat)
            mRightPaddingKey = (if(EnvironmentSingleton.instance.isLandscape) getInstance().internal.keyboardRightPaddingLandscapeFloat
            else getInstance().internal.keyboardRightPaddingFloat)
            bottomPadding = mBottomPaddingKey.getValue()
            rightPadding = mRightPaddingKey.getValue()
            mSkbRoot.bottomPadding = 0
            mSkbRoot.rightPadding = 0
            mLlKeyboardBottomHolder.minimumHeight = EnvironmentSingleton.instance.heightForKeyboardMove
            val mIvKeyboardMove = ImageView(context).apply {
                setImageResource(R.drawable.ic_horizontal_line)
                isClickable = true
                isEnabled = true
            }
            mLlKeyboardBottomHolder.addView(mIvKeyboardMove)
            mIvKeyboardMove.setOnTouchListener { _, event -> onMoveKeyboardEvent(event) }
        } else {
            val fullDisplayKeyboardEnable = getInstance().internal.fullDisplayKeyboardEnable.getValue()
            if(fullDisplayKeyboardEnable){
                mFullDisplayKeyboardBar = FullDisplayKeyboardBar(context, this)
                mLlKeyboardBottomHolder.addView(mFullDisplayKeyboardBar)
                mLlKeyboardBottomHolder.minimumHeight = EnvironmentSingleton.instance.heightForFullDisplayBar + EnvironmentSingleton.instance.systemNavbarWindowsBottom
            } else {
                mLlKeyboardBottomHolder.minimumHeight = EnvironmentSingleton.instance.systemNavbarWindowsBottom
            }
            bottomPadding = 0
            rightPadding = 0
            mBottomPaddingKey =  getInstance().internal.keyboardBottomPadding
            mRightPaddingKey =  getInstance().internal.keyboardRightPadding
            mSkbRoot.bottomPadding = mBottomPaddingKey.getValue()
            mSkbRoot.rightPadding = mRightPaddingKey.getValue()
        }
        updateTheme()
        DecodingInfo.candidatesLiveData.observe( this){ _ ->
            updateCandidateBar()
            (KeyboardManager.instance.currentContainer as? CandidatesContainer)?.showCandidatesView()
        }
    }

    private var initialTouchX = 0f
    private var initialTouchY = 0f
    private var rightPaddingValue = 0  // 右侧边距
    private var bottomPaddingValue = 0  // 底部边距
    private fun onMoveKeyboardEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                bottomPaddingValue = mBottomPaddingKey.getValue()
                rightPaddingValue = mRightPaddingKey.getValue()
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val dx: Float = event.rawX - initialTouchX
                val dy: Float = event.rawY - initialTouchY
                if(dx.absoluteValue > 10) {
                    rightPaddingValue -= dx.toInt()
                    rightPaddingValue = if(rightPaddingValue < 0) 0
                    else if(rightPaddingValue > EnvironmentSingleton.instance.mScreenWidth - mSkbRoot.width) {
                        EnvironmentSingleton.instance.mScreenWidth - mSkbRoot.width
                    } else rightPaddingValue
                    initialTouchX = event.rawX
                    if(EnvironmentSingleton.instance.keyboardModeFloat) {
                        rightPadding = rightPaddingValue
                    } else {
                        mSkbRoot.rightPadding = rightPaddingValue
                    }
                }
                if(dy.absoluteValue > 10 ) {
                    bottomPaddingValue -= dy.toInt()
                    bottomPaddingValue = if(bottomPaddingValue < 0) 0
                    else if(bottomPaddingValue > EnvironmentSingleton.instance.mScreenHeight - mSkbRoot.height) {
                        EnvironmentSingleton.instance.mScreenHeight - mSkbRoot.height
                    } else bottomPaddingValue
                    initialTouchY = event.rawY
                    if(EnvironmentSingleton.instance.keyboardModeFloat) {
                        bottomPadding = bottomPaddingValue
                    } else {
                        mSkbRoot.bottomPadding = bottomPaddingValue
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                mRightPaddingKey.setValue(rightPaddingValue)
                mBottomPaddingKey.setValue(bottomPaddingValue)
            }
        }
        return false
    }

    // 刷新主题
    fun updateTheme() {
        setBackgroundResource(android.R.color.transparent)
        val keyTextColor = ThemeManager.activeTheme.keyTextColor
        val backgrounde = ThemeManager.activeTheme.backgroundDrawable(ThemeManager.prefs.keyBorder.getValue())
        mSkbRoot.background = if(backgrounde is BitmapDrawable) BitmapDrawable(context.resources, Bitmap.createScaledBitmap(backgrounde.bitmap, EnvironmentSingleton.instance.skbWidth, EnvironmentSingleton.instance.inputAreaHeight, true)) else backgrounde
        mComposingView.updateTheme(ThemeManager.activeTheme)
        mSkbCandidatesBarView.updateTheme(keyTextColor)
        if(::mOnehandHoderLayout.isInitialized) {
            (mOnehandHoderLayout[0] as ImageButton).drawable?.setTint(keyTextColor)
            (mOnehandHoderLayout[1] as ImageButton).drawable?.setTint(keyTextColor)
        }
        mFullDisplayKeyboardBar?.updateTheme(keyTextColor)
        mAddPhrasesLayout.updateTheme(ThemeManager.activeTheme)
    }

    private fun onClick(view: View) {
        if (view.id == R.id.ib_holder_one_hand_none) {
            getInstance().keyboardSetting.oneHandedModSwitch.setValue(!getInstance().keyboardSetting.oneHandedModSwitch.getValue())
        } else {
            val oneHandedMod = getInstance().keyboardSetting.oneHandedMod.getValue()
            getInstance().keyboardSetting.oneHandedMod.setValue(if (oneHandedMod == KeyboardOneHandedMod.LEFT) KeyboardOneHandedMod.RIGHT else KeyboardOneHandedMod.LEFT)
        }
        EnvironmentSingleton.instance.initData()
        KeyboardLoaderUtil.instance.clearKeyboardMap()
        KeyboardManager.instance.clearKeyboard()
        KeyboardManager.instance.switchKeyboard(InputModeSwitcherManager.skbLayout)
    }

    /**
     * 响应软键盘按键的处理函数。在软键盘集装箱SkbContainer中responseKeyEvent（）的调用。
     * 软键盘集装箱SkbContainer的responseKeyEvent（）在自身类中调用。
     */
    override fun responseKeyEvent(sKey: SoftKey) {
        val keyCode = sKey.keyCode
        if (sKey.isKeyCodeKey) {  // 系统的keycode,单独处理
            mImeState = ImeState.STATE_INPUT
            val keyEvent = KeyEvent(0, 0, KeyEvent.ACTION_UP, keyCode, 0, 0, 0, 0, KeyEvent.FLAG_SOFT_KEYBOARD)
            processKey(keyEvent)
        } else if (sKey.isUserDefKey || sKey.isUniStrKey) { // 是用户定义的keycode
            if (!DecodingInfo.isAssociate && !DecodingInfo.isCandidatesListEmpty) {
                if(InputModeSwitcherManager.isChinese)   chooseAndUpdate()
                else if(InputModeSwitcherManager.isEnglish)  commitDecInfoText(DecodingInfo.composingStrForCommit)  // 把输入的拼音字符串发送给EditText
            }
            if (InputModeSwitcherManager.USER_DEF_KEYCODE_SYMBOL_3 == keyCode) {  // 点击标点按钮
                KeyboardManager.instance.switchKeyboard(KeyboardManager.KeyboardType.SYMBOL)
                (KeyboardManager.instance.currentContainer as? SymbolContainer)?.setSymbolsView()
            } else  if (InputModeSwitcherManager.USER_DEF_KEYCODE_EMOJI_4 == keyCode) {  // 点击表情按钮
                onSettingsMenuClick(SkbMenuMode.Emojicon)
            } else if ( keyCode in InputModeSwitcherManager.USER_DEF_KEYCODE_RETURN_6 .. InputModeSwitcherManager.USER_DEF_KEYCODE_SHIFT_1) {
                InputModeSwitcherManager.switchModeForUserKey(keyCode)
            }else if(sKey.keyLabel.isNotBlank()){
                if(SymbolPreset.containsKey(sKey.keyLabel))commitPairSymbol(sKey.keyLabel)
                else commitText(sKey.keyLabel)
            }
            if(mImeState != ImeState.STATE_IDLE) resetToIdleState()
        }
    }


    private var textBeforeCursor:String = ""

    /**
     * 响应软键盘长按键的处理函数。在软键盘集装箱SkbContainer中responseKeyEvent（）的调用。
     * 软键盘集装箱SkbContainer的responseKeyEvent（）在自身类中调用。
     */
    override fun responseLongKeyEvent(result:Pair<PopupMenuMode, String>) {
        if (!DecodingInfo.isAssociate && !DecodingInfo.isCandidatesListEmpty) {
            if(InputModeSwitcherManager.isChinese) {
                chooseAndUpdate()
            } else if(InputModeSwitcherManager.isEnglish){
                val displayStr = DecodingInfo.composingStrForCommit // 把输入的拼音字符串发送给EditText
                commitDecInfoText(displayStr)
            }
        }
        when(result.first){
            PopupMenuMode.Text -> {
                if(SymbolPreset.containsKey(result.second))commitPairSymbol(result.second)
                else commitText(result.second)
            }
            PopupMenuMode.SwitchIME -> InputMethodUtil.showPicker()
            PopupMenuMode.EnglishCell -> {
                getInstance().input.abcSearchEnglishCell.setValue(!getInstance().input.abcSearchEnglishCell.getValue())
                KeyboardManager.instance.switchKeyboard(InputModeSwitcherManager.skbLayout)
            }
            PopupMenuMode.Clear -> {
                if(isAddPhrases) mAddPhrasesLayout.clearPhrasesContent()
                else {
                    val inputConnection = service.getCurrentInputConnection()
                    val clearText = inputConnection.getTextBeforeCursor(1000, InputConnection.GET_TEXT_WITH_STYLES).toString()
                    if(clearText.isNotEmpty()){
                        textBeforeCursor = clearText
                        inputConnection.deleteSurroundingText(1000, 0)
                    }
                }
            }
            PopupMenuMode.Revertl -> {
                commitText(textBeforeCursor)
                textBeforeCursor = ""
            }
            PopupMenuMode.Enter ->  commitText("\n") // 长按回车键
            else -> {}
        }
        if(result.first == PopupMenuMode.Text && mImeState != ImeState.STATE_PREDICT) resetToPredictState()
        else if(result.first != PopupMenuMode.None && mImeState != ImeState.STATE_IDLE) resetToIdleState()
    }

    override fun responseHandwritingResultEvent(words: Array<CandidateListItem>) {
        DecodingInfo.isAssociate = false
        DecodingInfo.cacheCandidates(words)
        mImeState = ImeState.STATE_INPUT
        updateCandidateBar()
    }

    /**
     * 按键处理函数
     */
    fun processKey(event: KeyEvent): Boolean {
        // 功能键处理
        if (processFunctionKeys(event)) return true
        val englishCellDisable = InputModeSwitcherManager.isEnglish && !getInstance().input.abcSearchEnglishCell.getValue()
        val result = if(englishCellDisable){
            processEnglishKey(event)
        } else if (!InputModeSwitcherManager.mInputTypePassword &&(InputModeSwitcherManager.isEnglish || InputModeSwitcherManager.isChinese)) { // 中文、英语输入模式
            processInput(event)
        } else { // 数字、符号处理
            processEnglishKey(event)
        }
        return result
    }

    /**
     * 英文非智能输入处理函数
     */
    private fun processEnglishKey(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        var keyChar = event.unicodeChar
        val lable = keyChar.toChar().toString()
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            sendKeyEvent(keyCode)
            if(mImeState != ImeState.STATE_IDLE) resetToIdleState()
            return true
        } else if(keyCode in (KeyEvent.KEYCODE_A .. KeyEvent.KEYCODE_Z) ){
            if (!InputModeSwitcherManager.isEnglishLower) keyChar = keyChar - 'a'.code + 'A'.code
            commitText(keyChar.toChar().toString())
            return true
        } else if (keyCode != 0) {
            sendKeyEvent(keyCode)
            if(mImeState != ImeState.STATE_IDLE) resetToIdleState()
            return true
        } else if (lable.isNotEmpty()) {
            if(SymbolPreset.containsKey(lable))commitPairSymbol(lable)
            else commitText(lable)
            return true
        }
        return false
    }

    /**
     * 功能键处理函数
     */
    private fun processFunctionKeys(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (service.isInputViewShown) {
                requestHideSelf()
                return true
            }
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_SPACE) {
            if (DecodingInfo.isFinish || (DecodingInfo.isAssociate && !mSkbCandidatesBarView.isActiveCand())) {
                sendKeyEvent(keyCode)
                if(mImeState != ImeState.STATE_IDLE) resetToIdleState()
            } else {
                chooseAndUpdate()
            }
            return true
        } else if (keyCode == KeyEvent.KEYCODE_CLEAR) {
            if(mImeState != ImeState.STATE_IDLE) resetToIdleState()
            return true
        }  else if (keyCode == KeyEvent.KEYCODE_ENTER) {
            if (DecodingInfo.isFinish || DecodingInfo.isAssociate) {
                sendKeyEvent(keyCode)
            } else {
                commitDecInfoText(DecodingInfo.composingStrForCommit)
            }
            if(mImeState != ImeState.STATE_IDLE) resetToIdleState()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
            if(event.flags != KeyEvent.FLAG_SOFT_KEYBOARD && !DecodingInfo.isCandidatesListEmpty) {
                mSkbCandidatesBarView.updateActiveCandidateNo(keyCode)
            } else if (DecodingInfo.isFinish || DecodingInfo.isAssociate) {
                moveCursorPosition(keyCode)
            } else {
                chooseAndUpdate()
            }
            return  true
        }else if (keyCode == KeyEvent.KEYCODE_DEL && (InputModeSwitcherManager.mInputTypePassword || InputModeSwitcherManager.isNumberSkb)) {
            sendKeyEvent(keyCode)
            if(mImeState != ImeState.STATE_IDLE) resetToIdleState()
            return true
        }
        return false
    }

    /**
     * 按键处理函数
     */
    private fun processInput(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val keyChar = event.unicodeChar
        val lable = keyChar.toChar().toString()
        if (Character.isLetterOrDigit(keyChar) || keyCode == KeyEvent.KEYCODE_APOSTROPHE || keyCode == KeyEvent.KEYCODE_SEMICOLON){
            DecodingInfo.inputAction(event)
            updateCandidate()
            return true
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (DecodingInfo.isFinish || DecodingInfo.isAssociate) {
                sendKeyEvent(keyCode)
                if(mImeState != ImeState.STATE_IDLE) resetToIdleState()
            } else {
                DecodingInfo.deleteAction()
                updateCandidate()
            }
            return true
        } else if (keyCode != 0) {
            if (!DecodingInfo.isCandidatesListEmpty && !DecodingInfo.isAssociate) {
                chooseAndUpdate()
            }
            sendKeyEvent(keyCode)
            if(mImeState != ImeState.STATE_IDLE) resetToIdleState()
            return true
        } else if(lable.isNotEmpty()) {
            if (!DecodingInfo.isCandidatesListEmpty && !DecodingInfo.isAssociate) {
                chooseAndUpdate()
            }
            if(SymbolPreset.containsKey(lable))commitPairSymbol(lable)
            else commitText(lable)
            return true
        }
        return false
    }

    /**
     * 重置到空闲状态
     */
    fun resetToIdleState() {
        resetCandidateWindow()
        mComposingView.setDecodingInfo()
        mImeState = ImeState.STATE_IDLE
    }

    /**
     * 切换到联想状态
     */
    private fun resetToPredictState() {
        resetCandidateWindow()
        mComposingView.setDecodingInfo()
        mImeState = ImeState.STATE_PREDICT
    }

    /**
     * 选择候选词，并根据条件是否进行下一步的预报。
     * @param candId 选择索引
     */
    fun chooseAndUpdate(candId: Int = mSkbCandidatesBarView.getActiveCandNo()) {
        val candidate = DecodingInfo.getCandidate(candId)
        if(candidate?.comment == "📋"){  // 处理剪贴板或常用语
            commitDecInfoText(candidate.text)
            if(mImeState != ImeState.STATE_PREDICT)resetToPredictState()
        } else {
            val choice = DecodingInfo.chooseDecodingCandidate(candId)
            if (DecodingInfo.isEngineFinish || DecodingInfo.isAssociate) {  // 选择的候选词上屏
                commitDecInfoText(choice)
                KeyboardManager.instance.switchKeyboard(InputModeSwitcherManager.skbLayout)
                (KeyboardManager.instance.currentContainer as? T9TextContainer)?.updateSymbolListView()
                if(mImeState != ImeState.STATE_PREDICT)resetToPredictState()
            } else {  // 不上屏，继续选择
                if (!DecodingInfo.isFinish) {
                    if (InputModeSwitcherManager.isEnglish) setComposingText(DecodingInfo.composingStrForDisplay)
                    updateCandidateBar()
                    (KeyboardManager.instance.currentContainer as? T9TextContainer)?.updateSymbolListView()
                } else {
                    if(mImeState != ImeState.STATE_IDLE) resetToIdleState()
                }
            }
        }
    }

    /**
     * 刷新候选词，重新从词库进行获取。
     */
    private fun updateCandidate() {
        DecodingInfo.updateDecodingCandidate()
        if (!DecodingInfo.isFinish) {
            updateCandidateBar()
            (KeyboardManager.instance.currentContainer as? T9TextContainer)?.updateSymbolListView()
        } else {
            if(mImeState != ImeState.STATE_IDLE) resetToIdleState()
        }
        if (InputModeSwitcherManager.isEnglish)setComposingText(DecodingInfo.composingStrForDisplay)
    }

    /**
     * 显示候选词视图
     */
    fun updateCandidateBar() {
        mSkbCandidatesBarView.showCandidates()
        mComposingView.setDecodingInfo()
    }

    /**
     * 重置候选词区域
     */
    private fun resetCandidateWindow() {
        DecodingInfo.reset()
        updateCandidateBar()
        (KeyboardManager.instance.currentContainer as? T9TextContainer)?.updateSymbolListView()
    }

    /**
     * 当用户选择了候选词或者在候选词视图滑动了手势时的通知输入法。实现了候选词视图的监听器CandidateViewListener，
     * 有选择候选词的处理函数、隐藏键盘的事件
     */
    inner class ChoiceNotifier internal constructor() : CandidateViewListener {
        override fun onClickChoice(choiceId: Int) {
            DevicesUtils.tryPlayKeyDown()
            DevicesUtils.tryVibrate(KeyboardManager.instance.currentContainer)
            chooseAndUpdate(choiceId)
        }

        override fun onClickMore(level: Int) {
            if (level == 0) {
                onSettingsMenuClick(SkbMenuMode.CandidatesMore)
            } else {
                KeyboardManager.instance.switchKeyboard(InputModeSwitcherManager.skbLayout)
                (KeyboardManager.instance.currentContainer as? T9TextContainer)?.updateSymbolListView()
            }
        }

        override fun onClickMenu(skbMenuMode: SkbMenuMode) {
            onSettingsMenuClick(skbMenuMode)
        }

        override fun onClickClearCandidate() {
            if(mImeState != ImeState.STATE_IDLE) resetToIdleState()
            KeyboardManager.instance.switchKeyboard(InputModeSwitcherManager.skbLayout)
        }

        override fun onClickClearClipBoard() {
            DataBaseKT.instance.clipboardDao().deleteAll()
            (KeyboardManager.instance.currentContainer as? ClipBoardContainer)?.showClipBoardView(SkbMenuMode.ClipBoard)
        }
    }

    fun onSettingsMenuClick(skbMenuMode: SkbMenuMode, extra:String = "") {
        when (skbMenuMode) {
            SkbMenuMode.AddPhrases -> {
                isAddPhrases = true
                DataBaseKT.instance.phraseDao().deleteByContent(extra)
                KeyboardManager.instance.switchKeyboard(InputModeSwitcherManager.skbLayout)
                initView(context)
                mAddPhrasesLayout.setExtraData(extra)
            }
            SkbMenuMode.AI -> {
                // 如果当前已经是AI容器，则切换回输入键盘
                // 否则切换到AI功能视图并更新候选栏
                if (KeyboardManager.instance.currentContainer is AIContainer) {
                    KeyboardManager.instance.switchKeyboard(InputModeSwitcherManager.skbLayout)
                } else {
                    KeyboardManager.instance.switchKeyboard(KeyboardManager.KeyboardType.AI)
                    mSkbCandidatesBarView.showAI()
                }
            }
            else ->onSettingsMenuClick(this, skbMenuMode)
        }
        mSkbCandidatesBarView.initMenuView()
    }


    /**
     * 输入法状态: 空闲，输入，联想
     */
    enum class ImeState {
        STATE_IDLE, STATE_INPUT, STATE_PREDICT
    }

    /**
     * 选择拼音
     */
    fun selectPrefix(position: Int) {
        // 播放按键声音和震动
        DevicesUtils.tryPlayKeyDown()
        DevicesUtils.tryVibrate(this)
        DecodingInfo.selectPrefix(position)
        updateCandidate()
    }

    //常用符号、剪切板
    fun showSymbols(symbols: Array<String>) {
        mImeState = ImeState.STATE_INPUT
        val list = symbols.map { symbol-> CandidateListItem("📋", symbol) }.toTypedArray()
        DecodingInfo.cacheCandidates(list)
        DecodingInfo.isAssociate = true
        updateCandidateBar()
    }

    fun requestHideSelf() {
        service.requestHideSelf(0)
    }

    /**
     * 模拟按键点击
     */
    private fun sendKeyEvent(keyCode: Int) {
        if(isAddPhrases){
            mAddPhrasesLayout.sendKeyEvent(keyCode)
            when(keyCode){
                KeyEvent.KEYCODE_ENTER ->{
                    isAddPhrases = false
                    initView(context)
                    onSettingsMenuClick(SkbMenuMode.Phrases)
                }
            }
        } else {
            if (keyCode != KeyEvent.KEYCODE_ENTER) {
                service.sendDownUpKeyEvents(keyCode)
            } else {
                val inputConnection = service.getCurrentInputConnection()
                YuyanEmojiCompat.mEditorInfo?.run {
                    if (inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_NULL || imeOptions.hasFlag(EditorInfo.IME_FLAG_NO_ENTER_ACTION)) {
                        service.sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER)
                    } else if (!actionLabel.isNullOrEmpty() && actionId != EditorInfo.IME_ACTION_UNSPECIFIED) {
                        inputConnection.performEditorAction(actionId)
                    } else when (val action = imeOptions and EditorInfo.IME_MASK_ACTION) {
                        EditorInfo.IME_ACTION_UNSPECIFIED, EditorInfo.IME_ACTION_NONE -> service.sendDownUpKeyEvents(keyCode)
                        else -> inputConnection.performEditorAction(action)
                    }
                }
            }
        }
    }

    /**
     * 向输入框提交预选词
     */
    private fun setComposingText(text: CharSequence) {
        if(!isAddPhrases)service.getCurrentInputConnection()?.setComposingText(text, 1)
    }

    /**
     * 发送字符串给编辑框
     */
    private fun commitText(text: String) {
        if(isAddPhrases) mAddPhrasesLayout.commitText(text)
        else service.getCurrentInputConnection()?.commitText(StringUtils.converted2FlowerTypeface(text), 1)
    }

    /**
     * 发送成对符号给编辑框
     */
    private fun commitPairSymbol(text: String) {
        if(isAddPhrases) {
            mAddPhrasesLayout.commitText(text)
        } else {
            val ic = service.getCurrentInputConnection()
            if(getInstance().input.symbolPairInput.getValue()) {
                ic?.commitText(text + SymbolPreset[text]!!, 1)
                moveCursorPosition(KeyEvent.KEYCODE_DPAD_LEFT)
            } else ic?.commitText(text, 1)
        }
    }

    /**
     * 发送候选词字符串给编辑框
     */
    private fun commitDecInfoText(resultText: String?) {
        if(resultText == null) return
        if(isAddPhrases){
            mAddPhrasesLayout.commitText(resultText)
        } else {
            val inputConnection = service.getCurrentInputConnection()
            inputConnection.commitText(StringUtils.converted2FlowerTypeface(resultText), 1)
            if (InputModeSwitcherManager.isEnglish && DecodingInfo.isEngineFinish && getInstance().input.abcSpaceAuto.getValue() && StringUtils.isEnglishWord(resultText)) {
                inputConnection.commitText(" ", 1)
            }
        }
    }

    /**
     * 移动光标
     */
    private fun moveCursorPosition(keyCode:Int) {
        val inputConnection = service.getCurrentInputConnection()
        inputConnection.beginBatchEdit()
        val eventTime = SystemClock.uptimeMillis()
        inputConnection.sendKeyEvent(KeyEvent(eventTime, eventTime, KeyEvent.ACTION_DOWN, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE, InputDevice.SOURCE_KEYBOARD))
        inputConnection.sendKeyEvent(KeyEvent(eventTime, SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, keyCode, 0, 0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, KeyEvent.FLAG_SOFT_KEYBOARD or KeyEvent.FLAG_KEEP_TOUCH_MODE, InputDevice.SOURCE_KEYBOARD))
        inputConnection.endBatchEdit()
    }

    private fun initNavbarBackground(service: ImeService) {
        service.window.window!!.also {
            WindowCompat.setDecorFitsSystemWindows(it, false)
            it.navigationBarColor = Color.TRANSPARENT
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) it.isNavigationBarContrastEnforced = false
        }
        ViewCompat.setOnApplyWindowInsetsListener(this) { _, insets ->
            EnvironmentSingleton.instance.systemNavbarWindowsBottom = insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom
            val fullDisplayKeyboardEnable = getInstance().internal.fullDisplayKeyboardEnable.getValue()
            mLlKeyboardBottomHolder.minimumHeight = if(EnvironmentSingleton.instance.keyboardModeFloat)  0
            else if(fullDisplayKeyboardEnable) EnvironmentSingleton.instance.heightForFullDisplayBar + EnvironmentSingleton.instance.systemNavbarWindowsBottom
            else  EnvironmentSingleton.instance.systemNavbarWindowsBottom
            insets
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun onStartInputView(editorInfo: EditorInfo, restarting: Boolean) {
        InputModeSwitcherManager.requestInputWithSkb(editorInfo)
        KeyboardManager.instance.switchKeyboard(InputModeSwitcherManager.skbLayout)
        if(!restarting) {
            YuyanEmojiCompat.setEditorInfo(editorInfo)
            if (getInstance().clipboard.clipboardSuggestion.getValue()) {
                val lastClipboardTime = getInstance().internal.clipboardUpdateTime.getValue()
                if (System.currentTimeMillis() - lastClipboardTime <= clipboardItemTimeout * 1000) {
                    val lastClipboardContent = getInstance().internal.clipboardUpdateContent.getValue()
                    if (lastClipboardContent.isNotBlank()) {
                        showSymbols(arrayOf(lastClipboardContent))
                        getInstance().internal.clipboardUpdateTime.setValue(0L)
                    }
                }
            }
        }
    }

    fun onWindowShown() {
        chinesePrediction = getInstance().input.chinesePrediction.getValue()
    }

    fun onWindowHidden() {
        if(isAddPhrases){
            isAddPhrases = false
            mAddPhrasesLayout.addPhrasesHandle()
            initView(context)
        }
        if(mImeState != ImeState.STATE_IDLE) resetToIdleState()
    }

    fun onUpdateSelection() {
        if((chinesePrediction && mImeState != ImeState.STATE_IDLE) || InputModeSwitcherManager.isNumberSkb) {
            val inputConnection = service.getCurrentInputConnection()
            val textBeforeCursor = inputConnection.getTextBeforeCursor(100, 0).toString()
            if (textBeforeCursor.isNotBlank()) {
                val expressionEnd = CustomEngine.parseExpressionAtEnd(textBeforeCursor)
                if(!expressionEnd.isNullOrBlank()) {
                    if(expressionEnd.length < 100) {
                        val result = CustomEngine.expressionCalculator(textBeforeCursor, expressionEnd)
                        if (result.isNotEmpty()) showSymbols(result)
                    }
                } else if (StringUtils.isChineseEnd(textBeforeCursor)) {
                    DecodingInfo.isAssociate = true
                    DecodingInfo.getAssociateWord(if (textBeforeCursor.length > 10)textBeforeCursor.substring(textBeforeCursor.length - 10) else textBeforeCursor)
                    updateCandidate()
                    updateCandidateBar()
                }
            }
        }
    }
}