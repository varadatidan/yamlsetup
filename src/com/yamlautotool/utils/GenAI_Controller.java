package com.yamlautotool.utils;

import java.io.FileWriter;
import java.nio.file.Paths;

public class GenAI_Controller {

    public static void generateScenario(String userIntent) {
        String context = YamlReader.getLocatorsAsContext();
        
        String prompt = "You are a QA Lead. Create a YAML test script for our tool.\n" +
            "Available Locators: " + context + "\n" +
            "User Intent: " + userIntent + "\n" +
            "Rules: Output ONLY raw YAML. Use actions: click, input, hover, wait, screenshot.";

        String yamlCode = AIClient.callGemini(prompt);

        // Saves to your resources folder
        String path = Paths.get("resources", "ai_generated_test.yaml").toString();
        try (FileWriter writer = new FileWriter(path)) {
            writer.write(yamlCode);
            System.out.println("🤖 AI: I've created 'ai_generated_test.yaml' in your resources folder!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}