package com.example.zhiyanai.keyboard;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * AI服务类，用于处理与Moonshot Kimi API的通信
 */
public class AIService {
    private static final String TAG = "AIService";
    private static final String API_URL = "https://api.moonshot.cn/v1/chat/completions";
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    
    private String apiKey;
    private OkHttpClient client;
    private Context context;
    
    public interface AIResponseListener {
        void onSuccess(List<String> suggestions);
        void onFailure(String errorMessage);
    }
    
    public AIService(Context context, String apiKey) {
        this.context = context;
        this.apiKey = apiKey;
        
        // 配置OkHttpClient，设置超时时间
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * 发送用户输入到AI进行优化
     * @param userInput 用户输入的文本
     * @param listener 回调监听器
     */
    public void optimizeExpression(String userInput, AIResponseListener listener) {
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", "moonshot-v1-8k");
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 800);
            
            // 构建消息数组
            JSONArray messages = new JSONArray();
            
            // 系统消息
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", "你是一个文本优化助手，请根据用户输入的文本，提供3-5个优化后的表达方式，使其更加流畅、得体。" +
                    "直接返回优化后的表达，不要有任何解释或前缀，每个表达用换行符分隔。");
            messages.put(systemMessage);
            
            // 用户消息
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", "请优化以下文本：" + userInput);
            messages.put(userMessage);
            
            requestBody.put("messages", messages);
            
            // 创建请求
            RequestBody body = RequestBody.create(requestBody.toString(), JSON);
            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();
            
            // 发送异步请求
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "API请求失败: " + e.getMessage());
                    listener.onFailure("网络请求失败: " + e.getMessage());
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        listener.onFailure("API响应错误: " + response.code());
                        return;
                    }
                    
                    try {
                        String responseBody = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseBody);
                        
                        // 解析AI响应
                        JSONArray choices = jsonResponse.getJSONArray("choices");
                        if (choices.length() > 0) {
                            JSONObject choice = choices.getJSONObject(0);
                            JSONObject message = choice.getJSONObject("message");
                            String content = message.getString("content");
                            
                            // 将响应分割成多个建议
                            String[] suggestions = content.split("\\n");
                            List<String> suggestionList = new ArrayList<>();
                            
                            for (String suggestion : suggestions) {
                                suggestion = suggestion.trim();
                                if (!suggestion.isEmpty()) {
                                    // 移除可能的序号前缀（如"1. "、"- "等）
                                    suggestion = suggestion.replaceAll("^\\d+\\.\\s*|^-\\s*", "");
                                    suggestionList.add(suggestion);
                                }
                            }
                            
                            listener.onSuccess(suggestionList);
                        } else {
                            listener.onFailure("AI未返回有效建议");
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "解析API响应失败: " + e.getMessage());
                        listener.onFailure("解析响应失败: " + e.getMessage());
                    }
                }
            });
            
        } catch (JSONException e) {
            Log.e(TAG, "构建请求失败: " + e.getMessage());
            listener.onFailure("构建请求失败: " + e.getMessage());
        }
    }
} 