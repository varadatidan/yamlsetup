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

public class GridTestRunner extends Project_Reports {

    private ExtentTest currentNode;
    private int stepIdx = 1;

    // This method is called by the AppLauncher for each YAML in the queue
    public void executeYaml(String yamlFile) throws Exception {
        // We assume setupSuite() and setupBrowser() were already called by AppLauncher
        TestCase testCase = YamlReader.loadTestCase(yamlFile);
        
        // Use the inherited 'extent' object to create the test
        test = extent.createTest(testCase.name); 
        currentNode = test; 
        
        String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());

        for (Step step : testCase.steps) {
            if (step.name != null && !step.name.isEmpty()) {
                currentNode = test.createNode("<b>" + step.name + "</b>")
                		;
            }
            executeSingleStep(step, null, ts);
        }
    }

    private void executeSingleStep(Step step, Map<String, String> dataRow, String ts) {
        String resVal = (step.value != null) ? step.value : (step.url != null ? step.url : "");
        String resLoc = (step.locator != null) ? step.locator : "";
        performAction(step, resLoc, resVal, ts);
    }

    private void performAction(Step step, String locator, String value, String ts) {
        String shotName = String.format("%03d_%s_%s", stepIdx++, step.action.toUpperCase(), ts);

        try {
            switch (step.action.toLowerCase()) {
                case "navigate":
                case "silent_navigate":
                    driver.get(value);
                    if (!step.action.contains("silent")) {
                        waitForGridData();
                        currentNode.pass("Navigated: " + value, 
                            MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(shotName)).build());
                    }
                    break;

                case "hover":
                    WebElement target = wait.until(ExpectedConditions.visibilityOfElementLocated(getBy(locator)));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", target);
                    ((JavascriptExecutor) driver).executeScript("var ev = document.createEvent('MouseEvents'); ev.initEvent('mouseenter',true,false); arguments[0].dispatchEvent(ev);", target);
                    new Actions(driver).moveToElement(target).perform();
                    Thread.sleep(1500); 
                    currentNode.info("Hovered: " + locator, 
                        MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(shotName)).build());
                    break;

                case "click":
                case "silent_click":
                    WebElement el = wait.until(ExpectedConditions.elementToBeClickable(getBy(locator)));
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                    
                    if (!step.action.contains("silent")) {
                        Thread.sleep(2000); 
                        waitForGridData();
                        currentNode.pass("Clicked: " + locator, 
                            MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(shotName)).build());
                    }
                    break;

                case "full_traversal":
                    currentNode.info("Starting Full Grid Traversal...");
                    while (true) {
                        try {
                            WebElement nextBtn = driver.findElement(By.xpath("//button[@title='Go to next page']"));
                            if (!nextBtn.isEnabled() || nextBtn.getAttribute("disabled") != null) break;
                            nextBtn.click();
                            Thread.sleep(1500); 
                        } catch (Exception e) { break; }
                    }
                    currentNode.pass("Full Traversal Completed", 
                        MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot("TRAV_END")).build());
                    break;

                case "refresh":
                    driver.navigate().refresh();
                    currentNode.info("Page Refreshed");
                    break;

                case "wait":
                case "silent_wait":
                    Thread.sleep(Long.parseLong(value));
                    break;
            }
        } catch (Exception e) {
            currentNode.fail("<b>Step Failed:</b> " + e.getMessage(), 
                MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot("ERR_" + shotName)).build());
        }
    }

    private void waitForGridData() {
        try {
            wait.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//div[@role='row' and contains(@class, 'MuiDataGrid-row')]")));
            Thread.sleep(1000); 
        } catch (Exception e) { }
    }

    private By getBy(String locator) {
        if (locator.startsWith("xpath=")) return By.xpath(locator.substring(6));
        return By.id(locator);
    }
}