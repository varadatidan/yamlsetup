package com.yamlautotool.runner;

import reports.reports.Project_Reports;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.yamlautotool.model.*;
import com.yamlautotool.utils.YamlReader;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.util.Map;

public class TestRunner extends Project_Reports {
    private int stepCounter = 1;

    public void executeYaml(String yamlFile) throws Exception {
        TestCase testCase = YamlReader.loadTestCase(yamlFile);
        test = extent.createTest(testCase.name); 
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());

        for (Step step : testCase.steps) {
            if ("data_loop".equalsIgnoreCase(step.action)) {
                handleDataLoop(testCase, step, ts);
            } else {
                executeSingleStep(step, null, ts, test);
            }
        }
    }

    private void handleDataLoop(TestCase testCase, Step loopStep, String ts) {
        for (Map<String, String> dataRow : testCase.testData) {
            String firstVal = dataRow.values().iterator().next();
            log("🔄 Iteration: " + firstVal);
            com.aventstack.extentreports.ExtentTest iterNode = test.createNode("Iteration: " + firstVal);
            for (Step subStep : loopStep.steps) {
                executeSingleStep(subStep, dataRow, ts, iterNode);
            }
        }
    }

    private void executeSingleStep(Step step, Map<String, String> dataRow, String ts, com.aventstack.extentreports.ExtentTest node) {
        String val = (step.value != null) ? step.value : (step.url != null ? step.url : "");
        String loc = (step.locator != null) ? step.locator : "";

        if (dataRow != null) {
            for (Map.Entry<String, String> entry : dataRow.entrySet()) {
                val = val.replace("${" + entry.getKey() + "}", entry.getValue());
                loc = loc.replace("${" + entry.getKey() + "}", entry.getValue());
            }
        }
        performAction(step, loc, val, ts, node);
    }

    private void performAction(Step step, String loc, String val, String ts, com.aventstack.extentreports.ExtentTest node) {
        String action = step.action.toLowerCase();
        String fileName = String.format("%03d_%s", stepCounter++, action.toUpperCase());
        try {
            switch (action) {
                case "navigate":
                case "silent_navigate":
                    log("🌐 Navigating: " + val);
                    driver.get(val);
                    if (!action.contains("silent")) node.pass("Navigated: " + val, MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(fileName)).build());
                    break;
                case "input":
                    log("⌨️ Input '" + val + "' into " + loc);
                    WebElement input = wait.until(ExpectedConditions.elementToBeClickable(getBy(loc)));
                    input.click();
                    if (step.clear_before) { input.clear(); input.sendKeys(Keys.CONTROL + "a", Keys.DELETE); }
                    input.sendKeys(val);
                    if (step.send_enter) input.sendKeys(Keys.ENTER);
                    node.pass("Entered: " + val);
                    break;
                case "click":
                case "silent_click":
                    log("🖱️ Clicking: " + loc);
                    WebElement el = wait.until(ExpectedConditions.elementToBeClickable(getBy(loc)));
                    js.executeScript("arguments[0].click();", el);
                    if (!action.contains("silent")) node.pass("Clicked: " + loc, MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(fileName)).build());
                    break;
                case "wait":
                case "silent_wait":
                    log("⏳ Waiting " + val + "ms");
                    Thread.sleep(Long.parseLong(val));
                    break;
            }
        } catch (Exception e) {
            log("❌ Step Failed: " + e.getMessage());
            node.fail("Failed: " + e.getMessage(), MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot("ERR_" + fileName)).build());
        }
    }

    private By getBy(String l) { return l.startsWith("xpath=") ? By.xpath(l.substring(6)) : By.id(l); }
}