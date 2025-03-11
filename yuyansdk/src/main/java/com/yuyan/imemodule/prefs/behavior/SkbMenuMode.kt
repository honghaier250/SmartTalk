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
    
    //添加AI功能的枚举值,这是实现AI功能入口的第一步 
    AI;

    companion object : ManagedPreference.StringLikeCodec<SkbMenuMode> {
        override fun decode(raw: String): SkbMenuMode =
            SkbMenuMode.valueOf(raw)
    }
}