package com.yamlautotool.runner;

import reports.reports.Project_Reports;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.yamlautotool.model.*;
import com.yamlautotool.utils.YamlReader;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.util.Map;

public class TestRunner extends Project_Reports {

    public static void main(String[] args) {
        TestRunner runner = null;
        try {
            runner = new TestRunner();
            runner.executeYaml("create_project.yaml");
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
            if (testCase == null) throw new RuntimeException("Could not load YAML: " + yamlFile);
            
            test = extent.createTest(testCase.name); 
            String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());

            for (Step step : testCase.steps) {
                if ("data_loop".equalsIgnoreCase(step.action)) {
                    handleYamlDataLoop(testCase, step, ts);
                } else {
                    executeSingleStep(step, null, ts);
                }
            }
        } finally {
            cleanup(); 
        }
    }

    private void handleYamlDataLoop(TestCase testCase, Step loopStep, String ts) {
        if (testCase.testData != null) {
            for (Map<String, String> dataRow : testCase.testData) {
                if ("No".equalsIgnoreCase(dataRow.getOrDefault("runMode", "Yes"))) {
                    test.info("<b>Skipping:</b> " + dataRow.get("testCaseID"));
                    continue; 
                }
                for (Step subStep : loopStep.steps) {
                    executeSingleStep(subStep, dataRow, ts);
                }
            }
        }
    }

    private void executeSingleStep(Step step, Map<String, String> dataRow, String ts) {
        String resVal = (step.value != null) ? step.value : "";
        String resLoc = (step.locator != null) ? step.locator : "";

        if (dataRow != null) {
            for (Map.Entry<String, String> entry : dataRow.entrySet()) {
                String target = "${" + entry.getKey() + "}";
                resVal = resVal.replace(target, entry.getValue());
                resLoc = resLoc.replace(target, entry.getValue());
            }
        }
        resVal = resVal.replace("${timestamp}", ts);
        resLoc = resLoc.replace("${timestamp}", ts);
        performAction(step, resLoc, resVal, ts);
    }

    private void performAction(Step step, String locator, String value, String ts) {
        try {
            switch (step.action.toLowerCase()) {
                case "navigate":
                    driver.get(step.url != null ? step.url : value);
                    test.pass("<b>Navigated:</b> " + (step.url != null ? step.url : value));
                    break;

                case "input":
                    WebElement input = wait.until(ExpectedConditions.elementToBeClickable(getBy(locator)));
                    input.click(); 
                    Thread.sleep(800); 
                    input.sendKeys(Keys.CONTROL + "a", Keys.DELETE);
                    input.sendKeys(value);
                    if (step.send_enter) {
                        Thread.sleep(800);
                        input.sendKeys(Keys.TAB); 
                        Thread.sleep(400);
                        input.sendKeys(Keys.ENTER);
                    }
                    test.pass("<b>Entered:</b> " + value);
                    break;

                case "click":
                    Thread.sleep(500); 
                    WebElement element = wait.until(ExpectedConditions.elementToBeClickable(getBy(locator)));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                    test.pass("<b>Clicked:</b> " + locator);
                    break;

                case "validate":
                    boolean isDisplayed = driver.findElement(getBy(locator)).isDisplayed();
                    if (isDisplayed) test.pass("<b>Validation Successful:</b> Element visible.");
                    else test.fail("<b>Validation Failed:</b> Element hidden.");
                    break;

                case "tab":
                    new org.openqa.selenium.interactions.Actions(driver).sendKeys(Keys.TAB).perform();
                    test.pass("<b>Sent:</b> TAB key", MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot("Tab_" + ts)).build());
                    break;
            }
        } catch (Exception e) {
            test.fail("<b>Step Failed:</b> " + e.getMessage());
        }
    }

    private By getBy(String locator) {
        if (locator.startsWith("xpath=")) return By.xpath(locator.substring(6));
        if (locator.startsWith("css=")) return By.cssSelector(locator.substring(4));
        return By.id(locator);
    }
}