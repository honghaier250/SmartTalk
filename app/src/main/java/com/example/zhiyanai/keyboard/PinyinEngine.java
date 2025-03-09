package com.example.zhiyanai.keyboard;

import android.content.Context;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 拼音输入法引擎
 * 负责拼音转换、候选词管理等核心功能
 */
public class PinyinEngine {
    private static final String TAG = "PinyinEngine";
    
    private Context context;
    private Map<String, List<String>> pinyinDict; // 拼音词典（单字）
    private Map<String, List<String>> phrasesDict; // 词组词典
    private StringBuilder inputBuffer;            // 输入缓冲区
    private List<String> candidates;              // 候选词列表
    private boolean isInitialized;                // 是否已初始化
    
    // 分隔符常量
    private static final String SEPARATOR = "'";
    
    public PinyinEngine(Context context) {
        this.context = context;
        this.pinyinDict = new HashMap<>();
        this.phrasesDict = new HashMap<>();
        this.inputBuffer = new StringBuilder();
        this.candidates = new ArrayList<>();
        this.isInitialized = false;
        initDictionaries();
    }
    
    /**
     * 初始化拼音词典
     */
    private void initDictionaries() {
        boolean singleCharDictLoaded = loadSingleCharDict();
        boolean phrasesDictLoaded = loadPhrasesDict();
        
        // 如果词典加载失败，添加一些默认的拼音映射
        if (!singleCharDictLoaded) {
            Log.d(TAG, "单字词典加载失败，添加默认拼音映射");
            addDefaultSingleCharDict();
        }
        
        if (!phrasesDictLoaded) {
            Log.d(TAG, "词组词典加载失败，添加默认词组映射");
            addDefaultPhrasesDict();
        }
        
        isInitialized = pinyinDict.size() > 0 || phrasesDict.size() > 0;
        
        if (isInitialized) {
            Log.d(TAG, "拼音词典加载完成，单字: " + pinyinDict.size() + " 个拼音，词组: " + phrasesDict.size() + " 个词组");
        } else {
            Log.e(TAG, "拼音词典加载失败");
        }
        
        // 添加详细日志，输出一些词典内容示例
        if (singleCharDictLoaded && pinyinDict.size() > 0) {
            StringBuilder sb = new StringBuilder("单字词典样例: ");
            int count = 0;
            for (Map.Entry<String, List<String>> entry : pinyinDict.entrySet()) {
                if (count < 5) {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
                    count++;
                } else {
                    break;
                }
            }
            Log.d(TAG, sb.toString());
        }
        
        if (phrasesDictLoaded && phrasesDict.size() > 0) {
            StringBuilder sb = new StringBuilder("词组词典样例: ");
            int count = 0;
            for (Map.Entry<String, List<String>> entry : phrasesDict.entrySet()) {
                if (count < 5) {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append(", ");
                    count++;
                } else {
                    break;
                }
            }
            Log.d(TAG, sb.toString());
        }
    }
    
    /**
     * 加载单字拼音词典
     */
    private boolean loadSingleCharDict() {
        try {
            AssetManager assetManager = context.getAssets();
            // 检查文件是否存在
            String[] files = assetManager.list("");
            boolean fileExists = false;
            boolean simpleFileExists = false;
            
            for (String file : files) {
                if ("pinyin_dict.txt".equals(file)) {
                    fileExists = true;
                }
                if ("pinyin_dict_simple.txt".equals(file)) {
                    simpleFileExists = true;
                }
            }
            
            // 优先使用简化版词典
            String dictFileName = simpleFileExists ? "pinyin_dict_simple.txt" : 
                                 (fileExists ? "pinyin_dict.txt" : null);
            
            if (dictFileName == null) {
                Log.e(TAG, "单字拼音词典文件不存在");
                return false;
            }
            
            Log.d(TAG, "使用单字拼音词典文件: " + dictFileName);
            
            // 从assets目录加载拼音词典文件
            InputStream is = assetManager.open(dictFileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                // 跳过注释行和空行
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }
                
                // 解析拼音和对应的汉字
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String pinyin = parts[0].trim();
                    String[] chars = parts[1].split(",");
                    
                    List<String> charList = new ArrayList<>();
                    for (String c : chars) {
                        charList.add(c.trim());
                    }
                    
                    pinyinDict.put(pinyin, charList);
                    lineCount++;
                }
            }
            
            reader.close();
            is.close();
            
            Log.d(TAG, "成功加载单字拼音词典，共 " + lineCount + " 行");
            return lineCount > 0;
        } catch (IOException e) {
            Log.e(TAG, "加载单字拼音词典失败: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 加载词组拼音词典
     */
    private boolean loadPhrasesDict() {
        try {
            AssetManager assetManager = context.getAssets();
            // 检查文件是否存在
            String[] files = assetManager.list("");
            boolean fileExists = false;
            boolean simpleFileExists = false;
            
            for (String file : files) {
                if ("pinyin_phrases.txt".equals(file)) {
                    fileExists = true;
                }
                if ("pinyin_phrases_simple.txt".equals(file)) {
                    simpleFileExists = true;
                }
            }
            
            // 优先使用简化版词典
            String dictFileName = simpleFileExists ? "pinyin_phrases_simple.txt" : 
                                 (fileExists ? "pinyin_phrases.txt" : null);
            
            if (dictFileName == null) {
                Log.e(TAG, "词组拼音词典文件不存在");
                return false;
            }
            
            Log.d(TAG, "使用词组拼音词典文件: " + dictFileName);
            
            // 从assets目录加载词组词典文件
            InputStream is = assetManager.open(dictFileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                // 跳过注释行和空行
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }
                
                // 解析拼音和对应的词组
                String[] parts = line.split("=");
                if (parts.length == 2) {
                    String pinyinCombination = parts[0].trim();
                    String[] phrases = parts[1].split(",");
                    
                    List<String> phraseList = new ArrayList<>();
                    for (String phrase : phrases) {
                        phraseList.add(phrase.trim());
                    }
                    
                    phrasesDict.put(pinyinCombination, phraseList);
                    lineCount++;
                }
            }
            
            reader.close();
            is.close();
            
            Log.d(TAG, "成功加载词组拼音词典，共 " + lineCount + " 行");
            return lineCount > 0;
        } catch (IOException e) {
            Log.e(TAG, "加载词组拼音词典失败: " + e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 处理拼音输入
     * @param input 输入的字符
     */
    public void handleInput(char input) {
        if (isValidPinyinChar(input)) {
            inputBuffer.append(input);
            updateCandidates();
        } else if (input == '\'') {
            // 处理分隔符
            inputBuffer.append(SEPARATOR);
            updateCandidates();
        }
    }
    
    /**
     * 删除最后一个输入字符
     */
    public void deleteLastInput() {
        if (inputBuffer.length() > 0) {
            inputBuffer.deleteCharAt(inputBuffer.length() - 1);
            updateCandidates();
        }
    }
    
    /**
     * 清空输入缓冲区
     */
    public void clearInput() {
        inputBuffer.setLength(0);
        candidates.clear();
    }
    
    /**
     * 获取当前的候选词列表
     */
    public List<String> getCandidates() {
        return candidates;
    }
    
    /**
     * 更新候选词列表
     */
    private void updateCandidates() {
        candidates.clear();
        String pinyin = inputBuffer.toString();
        
        Log.d(TAG, "更新候选词，当前拼音: " + pinyin);
        
        if (!pinyin.isEmpty()) {
            // 1. 添加词组匹配
            addPhraseMatches(pinyin);
            
            // 2. 添加单字完全匹配
            addSingleCharMatches(pinyin);
            
            // 3. 添加拼音前缀匹配
            addPrefixMatches(pinyin);
            
            // 如果没有找到任何候选词，尝试添加一些默认候选词
            if (candidates.isEmpty()) {
                Log.d(TAG, "没有找到候选词，尝试添加默认候选词");
                // 添加拼音本身作为候选词
                candidates.add(pinyin);
                
                // 如果拼音长度为1，尝试添加一些常用字
                if (pinyin.length() == 1) {
                    char c = pinyin.charAt(0);
                    if (c == 'a') candidates.add("啊");
                    else if (c == 'b') candidates.add("不");
                    else if (c == 'c') candidates.add("从");
                    else if (c == 'd') candidates.add("的");
                    else if (c == 'e') candidates.add("额");
                    else if (c == 'f') candidates.add("发");
                    else if (c == 'g') candidates.add("个");
                    else if (c == 'h') candidates.add("和");
                    else if (c == 'i') candidates.add("一");
                    else if (c == 'j') candidates.add("就");
                    else if (c == 'k') candidates.add("可");
                    else if (c == 'l') candidates.add("了");
                    else if (c == 'm') candidates.add("吗");
                    else if (c == 'n') candidates.add("你");
                    else if (c == 'o') candidates.add("哦");
                    else if (c == 'p') candidates.add("平");
                    else if (c == 'q') candidates.add("去");
                    else if (c == 'r') candidates.add("人");
                    else if (c == 's') candidates.add("是");
                    else if (c == 't') candidates.add("他");
                    else if (c == 'u') candidates.add("有");
                    else if (c == 'v') candidates.add("为");
                    else if (c == 'w') candidates.add("我");
                    else if (c == 'x') candidates.add("下");
                    else if (c == 'y') candidates.add("一");
                    else if (c == 'z') candidates.add("在");
                }
            }
        }
        
        Log.d(TAG, "候选词列表: " + candidates);
    }
    
    /**
     * 添加词组匹配的候选词
     */
    private void addPhraseMatches(String pinyin) {
        Log.d(TAG, "尝试匹配词组，拼音: " + pinyin);
        
        // 检查是否有分隔符
        if (pinyin.contains(SEPARATOR)) {
            // 处理带分隔符的情况
            String[] parts = pinyin.split(SEPARATOR);
            StringBuilder combinedPinyin = new StringBuilder();
            
            for (int i = 0; i < parts.length; i++) {
                if (i > 0) combinedPinyin.append("+");
                combinedPinyin.append(parts[i]);
            }
            
            // 查找完全匹配的词组
            String pinyinCombination = combinedPinyin.toString();
            Log.d(TAG, "分隔符拼音组合: " + pinyinCombination);
            addPhrasesForPinyin(pinyinCombination);
            
            // 尝试匹配前缀
            for (String key : phrasesDict.keySet()) {
                if (key.startsWith(pinyinCombination) && !key.equals(pinyinCombination)) {
                    List<String> phrases = phrasesDict.get(key);
                    if (phrases != null && !phrases.isEmpty()) {
                        for (String phrase : phrases) {
                            candidates.add(phrase + "(" + key + ")");
                            
                            // 限制候选词数量
                            if (candidates.size() >= 10) {
                                break;
                            }
                        }
                    }
                    
                    // 限制候选词数量
                    if (candidates.size() >= 10) {
                        break;
                    }
                }
            }
        } else {
            // 尝试查找所有可能的词组组合
            for (String key : phrasesDict.keySet()) {
                // 检查是否以当前拼音开头
                if (key.startsWith(pinyin) || key.replace("+", "").startsWith(pinyin)) {
                    List<String> phrases = phrasesDict.get(key);
                    if (phrases != null && !phrases.isEmpty()) {
                        for (String phrase : phrases) {
                            candidates.add(phrase + "(" + key + ")");
                            
                            // 限制候选词数量
                            if (candidates.size() >= 10) {
                                break;
                            }
                        }
                    }
                    
                    // 限制候选词数量
                    if (candidates.size() >= 10) {
                        break;
                    }
                }
                
                // 检查是否以当前拼音的第一部分开头（针对多音节词组）
                if (key.contains("+")) {
                    String firstPart = key.substring(0, key.indexOf("+"));
                    if (firstPart.startsWith(pinyin) || pinyin.startsWith(firstPart)) {
                        List<String> phrases = phrasesDict.get(key);
                        if (phrases != null && !phrases.isEmpty()) {
                            for (String phrase : phrases) {
                                candidates.add(phrase + "(" + key + ")");
                                
                                // 限制候选词数量
                                if (candidates.size() >= 10) {
                                    break;
                                }
                            }
                        }
                        
                        // 限制候选词数量
                        if (candidates.size() >= 10) {
                            break;
                        }
                    }
                }
            }
        }
        
        Log.d(TAG, "词组匹配完成，候选词数量: " + candidates.size());
    }
    
    /**
     * 添加单字匹配的候选词
     */
    private void addSingleCharMatches(String pinyin) {
        Log.d(TAG, "尝试匹配单字，拼音: " + pinyin);
        
        // 如果有分隔符，处理最后一个拼音部分
        if (pinyin.contains(SEPARATOR)) {
            String[] parts = pinyin.split(SEPARATOR);
            String lastPart = parts[parts.length - 1];
            
            if (!lastPart.isEmpty()) {
                Log.d(TAG, "分隔符最后部分: " + lastPart);
                addCandidatesForPinyin(lastPart);
            }
            
            // 也处理每个部分
            for (String part : parts) {
                if (!part.isEmpty() && !part.equals(lastPart)) {
                    addCandidatesForPinyin(part);
                }
            }
        } else {
            // 没有分隔符，直接匹配
            addCandidatesForPinyin(pinyin);
        }
        
        Log.d(TAG, "单字匹配完成，候选词数量: " + candidates.size());
    }
    
    /**
     * 添加前缀匹配的候选词
     */
    private void addPrefixMatches(String pinyin) {
        Log.d(TAG, "尝试前缀匹配，拼音: " + pinyin);
        
        // 如果有分隔符，处理最后一个拼音部分
        String targetPinyin = pinyin;
        if (pinyin.contains(SEPARATOR)) {
            String[] parts = pinyin.split(SEPARATOR);
            targetPinyin = parts[parts.length - 1];
            Log.d(TAG, "分隔符最后部分: " + targetPinyin);
        }
        
        // 前缀匹配
        int prefixMatchCount = 0;
        for (String key : pinyinDict.keySet()) {
            if (key.startsWith(targetPinyin) && !key.equals(targetPinyin)) {
                List<String> chars = pinyinDict.get(key);
                if (chars != null && !chars.isEmpty()) {
                    // 只添加第一个字符作为候选
                    candidates.add(chars.get(0) + "(" + key + ")");
                    prefixMatchCount++;
                    
                    // 限制候选词数量
                    if (prefixMatchCount >= 10) {
                        break;
                    }
                }
            }
        }
        
        Log.d(TAG, "前缀匹配完成，匹配数量: " + prefixMatchCount);
    }
    
    /**
     * 为指定拼音添加词组候选词
     */
    private void addPhrasesForPinyin(String pinyinCombination) {
        List<String> phrases = phrasesDict.get(pinyinCombination);
        if (phrases != null && !phrases.isEmpty()) {
            Log.d(TAG, "找到词组完全匹配: " + pinyinCombination + " -> " + phrases);
            candidates.addAll(phrases);
        } else {
            Log.d(TAG, "未找到词组完全匹配: " + pinyinCombination);
        }
    }
    
    /**
     * 为指定拼音添加单字候选词
     */
    private void addCandidatesForPinyin(String pinyin) {
        // 完全匹配的拼音
        List<String> chars = pinyinDict.get(pinyin);
        if (chars != null && !chars.isEmpty()) {
            Log.d(TAG, "找到单字完全匹配: " + pinyin + " -> " + chars);
            candidates.addAll(chars);
        } else {
            Log.d(TAG, "未找到单字完全匹配: " + pinyin);
        }
    }
    
    /**
     * 检查字符是否为有效的拼音字符
     */
    private boolean isValidPinyinChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }
    
    /**
     * 获取当前输入的拼音字符串
     */
    public String getCurrentInput() {
        return inputBuffer.toString();
    }
    
    /**
     * 检查拼音引擎是否已初始化
     */
    public boolean isInitialized() {
        return isInitialized;
    }
    
    /**
     * 添加默认的单字拼音映射
     */
    private void addDefaultSingleCharDict() {
        // 添加常用汉字的拼音映射
        addPinyinMapping("a", "啊", "阿");
        addPinyinMapping("ai", "爱", "哀", "挨");
        addPinyinMapping("an", "安", "按", "案");
        addPinyinMapping("ba", "八", "爸", "吧");
        addPinyinMapping("bai", "白", "百", "败");
        addPinyinMapping("ban", "半", "办", "班");
        addPinyinMapping("bi", "比", "必", "笔");
        addPinyinMapping("bu", "不", "步", "部");
        addPinyinMapping("ca", "擦", "嚓");
        addPinyinMapping("cai", "才", "菜", "采");
        addPinyinMapping("de", "的", "得", "德");
        addPinyinMapping("di", "地", "第", "低");
        addPinyinMapping("dou", "都", "斗", "豆");
        addPinyinMapping("e", "饿", "鹅", "额");
        addPinyinMapping("er", "而", "二", "儿");
        addPinyinMapping("fa", "发", "法", "罚");
        addPinyinMapping("fan", "反", "饭", "范");
        addPinyinMapping("fang", "方", "放", "房");
        addPinyinMapping("gao", "高", "搞", "告");
        addPinyinMapping("ge", "个", "各", "革");
        addPinyinMapping("gong", "工", "公", "功");
        addPinyinMapping("hao", "好", "号", "浩");
        addPinyinMapping("he", "和", "合", "河");
        addPinyinMapping("hen", "很", "狠", "恨");
        addPinyinMapping("huo", "或", "活", "火");
        addPinyinMapping("ji", "几", "己", "计");
        addPinyinMapping("jia", "家", "加", "假");
        addPinyinMapping("jian", "见", "间", "件");
        addPinyinMapping("jiang", "将", "讲", "江");
        addPinyinMapping("jiao", "叫", "脚", "交");
        addPinyinMapping("jin", "进", "近", "今");
        addPinyinMapping("jiu", "就", "九", "旧");
        addPinyinMapping("kai", "开", "凯", "慨");
        addPinyinMapping("kan", "看", "刊", "勘");
        addPinyinMapping("ke", "可", "克", "科");
        addPinyinMapping("lai", "来", "赖");
        addPinyinMapping("le", "了", "乐", "勒");
        addPinyinMapping("li", "里", "力", "利");
        addPinyinMapping("ma", "吗", "妈", "马");
        addPinyinMapping("mei", "没", "每", "美");
        addPinyinMapping("men", "们", "门", "闷");
        addPinyinMapping("na", "那", "哪", "拿");
        addPinyinMapping("ni", "你", "尼", "逆");
        addPinyinMapping("nian", "年", "念", "粘");
        addPinyinMapping("qu", "去", "取", "区");
        addPinyinMapping("ren", "人", "认", "任");
        addPinyinMapping("ri", "日");
        addPinyinMapping("shi", "是", "时", "事");
        addPinyinMapping("shou", "手", "受", "收");
        addPinyinMapping("shuo", "说", "硕", "烁");
        addPinyinMapping("ta", "他", "她", "它");
        addPinyinMapping("tian", "天", "田", "添");
        addPinyinMapping("wo", "我", "握", "窝");
        addPinyinMapping("xi", "西", "系", "息");
        addPinyinMapping("xia", "下", "夏", "吓");
        addPinyinMapping("xian", "先", "线", "现");
        addPinyinMapping("xiang", "想", "向", "相");
        addPinyinMapping("xiao", "小", "笑", "校");
        addPinyinMapping("xie", "些", "写", "谢");
        addPinyinMapping("yi", "一", "以", "已");
        addPinyinMapping("yin", "因", "音", "引");
        addPinyinMapping("you", "有", "又", "由");
        addPinyinMapping("zai", "在", "再", "灾");
        addPinyinMapping("zhe", "这", "着", "者");
        addPinyinMapping("zheng", "正", "整", "证");
        addPinyinMapping("zhi", "之", "只", "知");
        addPinyinMapping("zhong", "中", "种", "重");
        addPinyinMapping("zi", "子", "自", "字");
        addPinyinMapping("zuo", "做", "作", "坐");
    }
    
    /**
     * 添加拼音映射
     */
    private void addPinyinMapping(String pinyin, String... chars) {
        List<String> charList = new ArrayList<>();
        for (String c : chars) {
            charList.add(c);
        }
        pinyinDict.put(pinyin, charList);
    }
    
    /**
     * 添加默认的词组拼音映射
     */
    private void addDefaultPhrasesDict() {
        // 添加常用词组的拼音映射
        addPhraseMapping("ni+hao", "你好", "尼耗");
        addPhraseMapping("wo+de", "我的", "窝的");
        addPhraseMapping("xie+xie", "谢谢", "泄泄");
        addPhraseMapping("zai+jian", "再见", "在见");
        addPhraseMapping("bu+yao", "不要", "不药");
        addPhraseMapping("ke+yi", "可以", "科仪");
        addPhraseMapping("zen+me", "怎么", "怎么");
        addPhraseMapping("duo+shao", "多少", "夺少");
        addPhraseMapping("shi+jian", "时间", "事件");
        addPhraseMapping("ming+tian", "明天", "明天");
        addPhraseMapping("jin+tian", "今天", "金田");
        addPhraseMapping("da+jia", "大家", "大家");
        addPhraseMapping("lao+shi", "老师", "老实");
        addPhraseMapping("xue+sheng", "学生", "学生");
        addPhraseMapping("peng+you", "朋友", "朋友");
        addPhraseMapping("ni+hao+ma", "你好吗", "你好嘛");
        addPhraseMapping("wo+ai+ni", "我爱你", "我爱你");
        addPhraseMapping("bu+ke+qi", "不客气", "不客气");
        addPhraseMapping("mei+guan+xi", "没关系", "没关系");
        addPhraseMapping("duo+shao+qian", "多少钱", "多少钱");
        addPhraseMapping("zhi+dao+le", "知道了", "知道了");
        addPhraseMapping("bu+zhi+dao", "不知道", "不知道");
    }
    
    /**
     * 添加词组映射
     */
    private void addPhraseMapping(String pinyinCombination, String... phrases) {
        List<String> phraseList = new ArrayList<>();
        for (String phrase : phrases) {
            phraseList.add(phrase);
        }
        phrasesDict.put(pinyinCombination, phraseList);
    }
}