package com.yamlautotool.runner;

import reports.reports.Project_Reports;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.aventstack.extentreports.ExtentTest;
import com.yamlautotool.model.*;
import com.yamlautotool.utils.YamlReader;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.util.Map;

public class TestRunner extends Project_Reports {

    private ExtentTest currentNode;
    private int stepCounter = 1; // Global counter to stop jumbling

    public static void main(String[] args) {
        TestRunner runner = null;
        try {
            runner = new TestRunner();
            runner.executeYaml("skill_assign.yaml");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (runner != null) {
                if (runner.driver != null) runner.driver.quit(); 
                if (runner.extent != null) runner.extent.flush();
            }
        }
    }

    public void executeYaml(String yamlFile) throws Exception {
        setupSuite();  
        setupBrowser(); 
        try {
            TestCase testCase = YamlReader.loadTestCase(yamlFile);
            test = extent.createTest(testCase.name); 
            currentNode = test; 
            String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());

            for (Step step : testCase.steps) {
                if (step.name != null && !step.name.isEmpty()) {
                    currentNode = test.createNode("<b>" + step.name + "</b>");
                }

                if ("data_loop".equalsIgnoreCase(step.action)) {
                    handleDataLoop(testCase, step, ts);
                } else {
                    executeSingleStep(step, null, ts);
                }
            }
        } finally {
            cleanup(); 
        }
    }

    private void handleDataLoop(TestCase testCase, Step loopStep, String ts) {
        if (testCase.testData != null) {
            for (Map<String, String> dataRow : testCase.testData) {
                String firstVal = dataRow.values().iterator().next();
                ExtentTest iterationNode = currentNode.createNode("Iteration: " + firstVal);
                
                // Switch focus to iteration node
                ExtentTest parent = currentNode;
                currentNode = iterationNode;
                
                for (Step subStep : loopStep.steps) {
                    executeSingleStep(subStep, dataRow, ts);
                }
                
                currentNode = parent; // Switch back
            }
        }
    }

    private void executeSingleStep(Step step, Map<String, String> dataRow, String ts) {
        String resVal = (step.value != null) ? step.value : (step.url != null ? step.url : "");
        String resLoc = (step.locator != null) ? step.locator : "";

        if (dataRow != null) {
            for (Map.Entry<String, String> entry : dataRow.entrySet()) {
                String key = "${" + entry.getKey() + "}";
                resVal = resVal.replace(key, entry.getValue());
                resLoc = resLoc.replace(key, entry.getValue());
            }
        }
        performAction(step, resLoc, resVal, ts);
    }

    private void performAction(Step step, String locator, String value, String ts) {
        String fileName = String.format("%03d_%s_%s", stepCounter++, step.action.toUpperCase(), ts);
        
        try {
            switch (step.action.toLowerCase()) {
                case "navigate":
                case "silent_navigate":
                    driver.get(value);
                    if (!step.action.contains("silent")) {
                        waitForGridData();
                        currentNode.pass("Navigated: " + value, MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(fileName)).build());
                    }
                    break;

                case "input":
                    WebElement input = wait.until(ExpectedConditions.elementToBeClickable(getBy(locator)));
                    input.click();
                    if (step.clear_before) input.sendKeys(Keys.CONTROL + "a", Keys.DELETE);
                    input.sendKeys(value);
                    if (step.send_enter) input.sendKeys(Keys.ENTER);
                    Thread.sleep(1500); 
                    currentNode.pass("Entered: " + value, MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(fileName)).build());
                    break;

                case "click":
                case "silent_click":
                    WebElement el = wait.until(ExpectedConditions.elementToBeClickable(getBy(locator)));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                    if (!step.action.contains("silent")) {
                        Thread.sleep(1500);
                        waitForGridData();
                        currentNode.pass("Clicked: " + locator, MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(fileName)).build());
                    }
                    break;

                case "custom_get_grid_count":
                    waitForGridData();
                    WebElement footer = wait.until(ExpectedConditions.visibilityOfElementLocated(getBy(locator)));
                    currentNode.pass("<b>Result:</b> " + value + " | " + footer.getText(), 
                        MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(fileName)).build());
                    break;

                case "wait":
                case "silent_wait":
                    Thread.sleep(Long.parseLong(value));
                    break;
            }
        } catch (Exception e) {
            currentNode.fail("Failed: " + e.getMessage(), MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot("ERR_" + fileName)).build());
        }
    }

    private void waitForGridData() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@role='row' and contains(@class, 'MuiDataGrid-row')]")));
            Thread.sleep(1000); 
        } catch (Exception e) { }
    }

    private By getBy(String locator) {
        if (locator.startsWith("xpath=")) return By.xpath(locator.substring(6));
        return By.id(locator);
    }
}