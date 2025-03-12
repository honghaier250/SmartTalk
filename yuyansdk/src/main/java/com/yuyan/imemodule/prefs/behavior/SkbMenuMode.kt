package com.yuyan.imemodule.prefs.behavior

import com.yuyan.imemodule.view.preference.ManagedPreference

enum class SkbMenuMode {
    SwitchKeyboard,
    KeyboardHeight,
    DarkTheme,
    Feedback,
    NumberRow,
    JianFan,
    LockEnglish,
    SymbolShow,
    CandidatesMore,
    Mnemonic,
    FlowerTypeface,
    EmojiInput,
    Handwriting,
    Custom,
    SettingsMenu,
    Settings,
    FloatKeyboard,
    OneHanded,
    PinyinT9,
    Pinyin26Jian,
    PinyinLx17,
    PinyinHandWriting,
    Pinyin26Double,
    PinyinStroke,
    ClipBoard,
    ClearClipBoard,
    Phrases,
    AddPhrases,
    CloseSKB,
    Emojicon,
    Emoticon,
    LockClipBoard,
    
    /**
     * AI功能的枚举值
     * 用于在键盘顶部操作栏中显示AI功能入口
     * 该枚举值与SkbFunData.kt中的菜单项定义相对应
     * 在CandidatesBar.kt和CandidatesMenuAdapter.kt中被引用
     */
    AI;

    companion object : ManagedPreference.StringLikeCodec<SkbMenuMode> {
        override fun decode(raw: String): SkbMenuMode =
            SkbMenuMode.valueOf(raw)
    }
}