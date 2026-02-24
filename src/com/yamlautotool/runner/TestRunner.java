package com.yamlautotool.runner;

import reports.reports.Project_Reports;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.yamlautotool.model.*;
import com.yamlautotool.utils.YamlReader;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.util.Map;
import java.util.List;

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
                if (runner.driver != null) {
                    runner.driver.quit(); 
                    System.out.println("Browser closed successfully.");
                }
                if (runner.extent != null) {
                    runner.extent.flush();
                }
            }
        }
    }

    public void executeYaml(String yamlFile) throws Exception {
        setupSuite();  
        setupBrowser(); 
        try {
            TestCase testCase = YamlReader.loadTestCase(yamlFile);
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
                    test.info("<b>Skipped:</b> " + dataRow.get("testCaseID"));
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
                        Thread.sleep(800); // Wait for filter results
                        input.sendKeys(Keys.TAB); 
                        Thread.sleep(400);
                        input.sendKeys(Keys.ENTER);
                        
                        // Fallback click logic if the dropdown remains open
                        try {
                            List<WebElement> options = driver.findElements(By.xpath("//div[@role='listbox']//li | //div[contains(@class,'listbox')]//li"));
                            if (!options.isEmpty()) {
                                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", options.get(0));
                            }
                        } catch (Exception e) {}
                    }
                    test.pass("<b>Entered & Selected:</b> " + value);
                    break;

                case "mui_dropdown":
                    selectCustomDropdown(value);
                    Thread.sleep(500); 
                    test.pass("<b>Selected Option:</b> " + value);
                    break;

                case "click":
                    Thread.sleep(500); 
                    WebElement element = wait.until(ExpectedConditions.elementToBeClickable(getBy(locator)));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                    test.pass("<b>Clicked:</b> " + locator);
                    break;

                case "screenshot":
                    String path = takeScreenshot(value + "_" + ts);
                    test.pass("<b>Visual Check:</b> " + value, MediaEntityBuilder.createScreenCaptureFromPath(path).build());
                    break;

                case "validate_mui_alert":
                    handleMuiValidation(value, ts);
                    break;

                case "wait":
                    Thread.sleep(Long.parseLong(value));
                    break;
                case "tab":
                    try {
                        Actions action = new Actions(driver);
                        // If a locator is provided, click it first to ensure focus before TAB
                        if (locator != null && !locator.isEmpty()) {
                            WebElement target = wait.until(ExpectedConditions.elementToBeClickable(getBy(locator)));
                            target.click();
                        }
                        
                        // Perform the TAB key action
                        action.sendKeys(Keys.TAB).build().perform();
                        test.info("<b>Sent Key:</b> TAB");

                        // Take the screenshot as per your requirement
                        String tabPath = takeScreenshot(value + "_" + ts);
                        test.pass("<b>Visual Check (Tab):</b> " + value, 
                            MediaEntityBuilder.createScreenCaptureFromPath(tabPath).build());
                    } catch (Exception e) {
                        test.fail("<b>Tab Action Failed:</b> " + e.getMessage());
                    }

            }
        } catch (Exception e) {
            test.fail("<b>Step Failed:</b> " + e.getMessage());
        }
    }

    private void handleMuiValidation(String expectedMsg, String ts) throws Exception {
        try {
            WebDriverWait shortWait = new WebDriverWait(driver, java.time.Duration.ofSeconds(5));
            WebElement alert = shortWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".MuiAlert-message")));
            String actualText = alert.getText().trim();
            String path = takeScreenshot("Alert_" + ts);
            if (actualText.contains(expectedMsg)) {
                test.pass("<b>Alert Match:</b> " + actualText, MediaEntityBuilder.createScreenCaptureFromPath(path).build());
            } else {
                test.fail("<b>Alert Mismatch!</b> Expected: " + expectedMsg + " | Actual: " + actualText, MediaEntityBuilder.createScreenCaptureFromPath(path).build());
            }
        } catch (Exception e) {
            java.util.List<WebElement> errors = driver.findElements(By.cssSelector(".text-xs.text-red-500.mt-1"));
            if (!errors.isEmpty()) {
                test.info("🔴 Field Validation Errors Visible");
            } else {
                test.fail("No alert found.");
            }
        }
    }

    private By getBy(String locator) {
        if (locator.startsWith("xpath=")) return By.xpath(locator.substring(6));
        if (locator.startsWith("css=")) return By.cssSelector(locator.substring(4));
        return By.id(locator);
    }
}