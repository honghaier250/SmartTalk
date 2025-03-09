# 智言AI输入法

智言AI输入法是一款集成了拼音输入和AI优化功能的Android输入法应用。它不仅支持中英文输入切换，还能通过调用大模型API来优化用户输入的文本表达。

## 主要功能

### 拼音输入功能
- 支持中英文输入模式切换
- 支持单字、词组输入
- 支持拼音分隔符(')输入
- 支持前缀匹配和模糊匹配
- 内置丰富的拼音词库

### AI优化功能
- 集成Moonshot Kimi大模型API
- 一键优化当前输入文本
- 提供多个优化建议供用户选择
- 优化结果直接替换原文本

## 使用方法

### 安装应用
1. 下载并安装应用
2. 在系统设置中启用"智言AI输入法"
3. 切换到智言AI输入法作为默认输入法

### 拼音输入
- 点击"中/英"按钮切换中英文输入模式
- 在中文模式下输入拼音，选择候选词
- 使用分隔符(')区分多音节拼音

### AI优化
1. 在输入框中输入文本
2. 点击键盘上的"AI"按钮
3. 等待几秒钟，系统会显示AI优化后的表达建议
4. 点击任意建议，系统会用选中的建议替换原文本

## 配置API密钥

**重要提示：** 使用AI优化功能前，您需要配置Moonshot Kimi API密钥。

1. 访问 [Moonshot平台](https://platform.moonshot.cn/) 注册并获取API密钥
2. 打开文件 `app/src/main/java/com/example/zhiyanai/keyboard/ZhiYanInputMethodService.java`
3. 找到第29行左右的API_KEY常量定义：
   ```java
   private static final String API_KEY = "YOUR_API_KEY";
   ```
4. 将"YOUR_API_KEY"替换为您的实际Moonshot API密钥
5. 重新编译并安装应用

## 技术架构

### 核心组件
- `ZhiYanInputMethodService`: 输入法服务主类
- `PinyinEngine`: 拼音引擎，处理拼音输入和候选词生成
- `AIService`: AI服务类，处理与Moonshot API的通信
- `CandidatesView`: 候选词视图
- `AISuggestionsView`: AI建议视图

### 词库文件

应用的assets目录下包含以下词库文件：

#### 拼音单字词典
- `pinyin_dict_simple.txt`: 简化版拼音单字词典（9.4KB，309行）
- `pinyin_dict.txt`: 标准版拼音单字词典（7.3KB，309行）

这些文件采用以下格式：
```
拼音=汉字1,汉字2,汉字3,...
```

例如：
```
ai=爱,哀,挨,埃,唉,哎,艾
ba=八,巴,爸,吧,拔,叭,芭,捌
```

#### 拼音词组词典
- `pinyin_phrases_simple.txt`: 简化版拼音词组词典（2.3KB，85行）
- `pinyin_phrases.txt`: 标准版拼音词组词典（2.3KB，85行）

这些文件采用以下格式：
```
拼音1+拼音2+...=词组1,词组2,...
```

例如：
```
ni+hao=你好,尼耗
wo+de=我的,窝的
```

应用会优先加载简化版词典（`*_simple.txt`），如果加载失败则尝试加载标准版词典。如果两者都加载失败，应用会使用内置的默认词库确保基本功能可用。

用户可以通过修改这些词库文件来自定义输入法的候选词，例如添加常用词组或专业术语。修改后需要重新安装应用或清除应用数据才能生效。

### 项目结构
```
app/
├── src/
│   ├── main/
│   │   ├── java/com/example/zhiyanai/
│   │   │   ├── keyboard/
│   │   │   │   ├── ZhiYanInputMethodService.java  # 输入法服务主类
│   │   │   │   ├── PinyinEngine.java              # 拼音引擎
│   │   │   │   ├── AIService.java                 # AI服务
│   │   │   │   ├── CandidatesView.java            # 候选词视图
│   │   │   │   └── AISuggestionsView.java         # AI建议视图
│   │   │   └── MainActivity.java                  # 主活动
│   │   ├── res/                                   # 资源文件
│   │   └── assets/                                # 词库文件
│   │       ├── pinyin_dict_simple.txt             # 拼音单字词典（简化版）
│   │       ├── pinyin_dict.txt                    # 拼音单字词典（标准版）
│   │       ├── pinyin_phrases_simple.txt          # 拼音词组词典（简化版）
│   │       └── pinyin_phrases.txt                 # 拼音词组词典（标准版）
│   └── ...
└── ...
```

### 依赖项
- OkHttp: 用于网络请求
- AndroidX: Android支持库

## 许可证

本项目采用MIT许可证。详情请参阅LICENSE文件。



