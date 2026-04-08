package com.yamlautotool.utils;

import okhttp3.*;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AIClient {

    // 1. YOUR API KEY FROM THE SCREENSHOT
    private static final String API_KEY = "YOUR_KEY";
    
    // 2. UPDATED 2026 URL (Using Gemini 2.5 Flash)
    private static final String GEMINI_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY;

    public static String callGemini(String prompt) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();

        JSONObject jsonBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", prompt);
        
        contents.put(new JSONObject().put("parts", new JSONArray().put(part)));
        jsonBody.put("contents", contents);

        RequestBody body = RequestBody.create(
                jsonBody.toString(),
                MediaType.get("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(GEMINI_URL)
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            String responseData = response.body().string();
            
            if (!response.isSuccessful()) {
                // This will now print the EXACT reason if it fails
                System.err.println("🚨 API ERROR: " + response.code() + " - " + responseData);
                return "ERROR_API_FAILED";
            }

            JSONObject jsonResponse = new JSONObject(responseData);
            return jsonResponse.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                    .replaceAll("(?i)```yaml|```", "").trim();

        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR_EXCEPTION";
        }
    }
}