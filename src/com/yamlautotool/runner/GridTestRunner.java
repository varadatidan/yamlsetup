package com.yamlautotool.runner;

import reports.reports.Project_Reports;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.ExtentTest;
import com.yamlautotool.model.*;
import com.yamlautotool.utils.YamlReader;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.io.File;
import java.time.Duration;

public class GridTestRunner extends Project_Reports {
    private ExtentTest currentNode;
    private int stepIdx;

    /**
     * Standard executeYaml method called by AppLauncher.
     */
    public void executeYaml(String yamlFile) throws Exception {
        this.stepIdx = 1;
        TestCase testCase = YamlReader.loadTestCase(yamlFile);
        test = extent.createTest("Grid Validation: " + new File(yamlFile).getName());
        currentNode = test;

        for (Step step : testCase.steps) {
            if (step.name != null && !step.name.isEmpty()) {
                currentNode = test.createNode("<b>" + step.name + "</b>");
            }
            String val = (step.value != null) ? step.value : step.url;
            performAction(step, (step.locator != null ? step.locator : ""), (val != null ? val : ""));
        }
    }

    private void performAction(Step step, String loc, String val) {
        String action = step.action.toLowerCase();
        String shot = String.format("%03d_%s", stepIdx++, action.toUpperCase());
        
        try {
            switch (action) {
                
                case "navigate":
                case "silent_navigate":
                    log("🌐 Navigating to URL: " + val);
                    driver.get(val); 
                    Thread.sleep(5000); 
                    if (!action.contains("silent")) {
                        currentNode.pass("Navigated to: " + val, 
                            MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(shot)).build());
                    }
                    break;

                case "click":
                case "silent_click":
                    log("🖱️ Clicking Element: " + loc);
                    WebElement clickEl = wait.until(ExpectedConditions.elementToBeClickable(getBy(loc)));
                    js.executeScript("arguments[0].click();", clickEl);
                    if (!action.contains("silent")) {
                        Thread.sleep(1000);
                        currentNode.pass("Clicked: " + loc, 
                            MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(shot)).build());
                    }
                    break;

                case "hover":
                    log("☁️ Hovering over: " + loc);
                    WebElement hovEl = wait.until(ExpectedConditions.visibilityOfElementLocated(getBy(loc)));
                    new Actions(driver).moveToElement(hovEl).pause(Duration.ofMillis(1000)).perform();
                    currentNode.info("Hovered over: " + loc);
                    break;

                case "refresh":
                    log("🔄 Refreshing page...");
                    driver.navigate().refresh();
                    Thread.sleep(8000); 
                    currentNode.info("Page Refreshed");
                    break;

                case "wait":
                case "silent_wait":
                    log("⏳ Sleeping for " + val + " ms");
                    Thread.sleep(Long.parseLong(val));
                    break;

                case "input":
                    log("⌨️ Typing '" + val + "' into " + loc);
                    WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(getBy(loc)));
                    if (step.clear_before) input.clear();
                    input.sendKeys(val);
                    if (step.send_enter) input.sendKeys(Keys.ENTER);
                    currentNode.pass("Input value: " + val);
                    break;

                case "full_traversal":
                    log("📜 Executing Full Grid Traversal (Scroll to Bottom)");
                    js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                    Thread.sleep(2000);
                    currentNode.pass("Full Traversal completed", 
                        MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(shot)).build());
                    break;

                default:
                    log("⚠️ Action '" + action + "' not recognized.");
                    break;
            }
        } catch (Exception e) {
            log("❌ Step Failed: " + e.getMessage());
            currentNode.fail("<b>Step Failed:</b> " + e.getMessage(), 
                MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot("ERR_" + shot)).build());
        }
    }

    private By getBy(String l) {
        if (l.startsWith("xpath=")) return By.xpath(l.substring(6));
        return By.id(l);
    }
}