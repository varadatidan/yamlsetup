package com.yamlautotool.runner;

import reports.reports.Project_Reports;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.ExtentTest;
import com.yamlautotool.model.*;
import com.yamlautotool.utils.YamlReader;

import java.text.SimpleDateFormat;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class GridTestRunner extends Project_Reports {
    private ExtentTest currentNode;
    private int stepIdx = 1;

    public void executeYaml(String yamlFile) throws Exception {
        TestCase testCase = YamlReader.loadTestCase(yamlFile);
        test = extent.createTest(testCase.name); 
        currentNode = test;
        String ts = new SimpleDateFormat("HHmmss").format(new java.util.Date());

        for (Step step : testCase.steps) {
            if (step.name != null && !step.name.isEmpty()) {
                currentNode = test.createNode("<b>" + step.name + "</b>");
            }
            performAction(step, (step.locator != null ? step.locator : ""), (step.value != null ? step.value : ""), ts);
        }
    }

    private void performAction(Step step, String locator, String value, String ts) {
        String shotName = String.format("%03d_%s", stepIdx++, step.action.toUpperCase());
        try {
            switch (step.action.toLowerCase()) {
                case "navigate":
                case "silent_navigate":
                    driver.get(value);
                    if (!step.action.contains("silent")) {
                        currentNode.pass("Navigated to: " + value, MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(shotName)).build());
                    }
                    break;
                case "click":
                case "silent_click":
                    WebElement el = wait.until(ExpectedConditions.elementToBeClickable(getBy(locator)));
                    js.executeScript("arguments[0].click();", el);
                    if (!step.action.contains("silent")) {
                        Thread.sleep(2000);
                        currentNode.pass("Clicked: " + locator, MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(shotName)).build());
                    }
                    break;
                case "input":
                    WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(getBy(locator)));
                    if (step.clear_before) input.clear();
                    input.sendKeys(value);
                    if (step.send_enter) input.sendKeys(Keys.ENTER);
                    currentNode.pass("Input '" + value + "' into " + locator);
                    break;
                case "wait":
                case "silent_wait":
                    Thread.sleep(Long.parseLong(value));
                    break;
            }
        } catch (Exception e) {
            currentNode.fail("Step Failed: " + e.getMessage(), MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot("ERR_" + shotName)).build());
        }
    }

    private By getBy(String locator) {
        if (locator.startsWith("xpath=")) return By.xpath(locator.substring(6));
        return By.id(locator);
    }
}