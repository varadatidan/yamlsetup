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
    private int stepIdx = 1; // Ensures chronological order in screenshots

    public static void main(String[] args) {
        GridTestRunner runner = null;
        try {
            runner = new GridTestRunner();
            // Point this to your Companies Grid YAML
			/*
			 * runner.executeYaml("project_grid_full.yaml");
			 * runner.executeYaml("companies_grid.yaml");
			 * runner.executeYaml("resources_grid.yaml");
			 * runner.executeYaml("skill_assignment.yaml");
			 * runner.executeYaml("skill_categories.yaml");
			 * runner.executeYaml("skills.yaml"); runner.executeYaml("practice_roles.yaml");
			 * runner.executeYaml("practice_master_grid.yaml");
			 * runner.executeYaml("holiday_grid.yaml");
			 * runner.executeYaml("users_grid_validation.yaml");
			 */ 
//            runner.executeYaml("roles_grid_validation.yaml");
//			 runner.executeYaml("data_imports_grid_validation.yaml");
			 runner.executeYaml("email_templates_grid_validation.yaml");
			 runner.executeYaml("notification_templates_grid_validation.yaml");

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
                // Creates a new section/folder in the report whenever 'name' is found
                if (step.name != null && !step.name.isEmpty()) {
                    currentNode = test.createNode("<b>" + step.name + "</b>");
                }
                executeSingleStep(step, null, ts);
            }
        } finally {
            cleanup(); 
        }
    }

    private void executeSingleStep(Step step, Map<String, String> dataRow, String ts) {
        String resVal = (step.value != null) ? step.value : (step.url != null ? step.url : "");
        String resLoc = (step.locator != null) ? step.locator : "";
        performAction(step, resLoc, resVal, ts);
    }

    private void performAction(Step step, String locator, String value, String ts) {
        // Unique name: e.g., 001_CLICK_20260306.png
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
                    // Force 'mouseenter' event to wake up hidden MUI buttons
                    ((JavascriptExecutor) driver).executeScript("var ev = document.createEvent('MouseEvents'); ev.initEvent('mouseenter',true,false); arguments[0].dispatchEvent(ev);", target);
                    new Actions(driver).moveToElement(target).perform();
                    Thread.sleep(1500); // Wait for menu to render
                    currentNode.info("Hovered: " + locator, 
                        MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(shotName)).build());
                    break;

                case "click":
                case "silent_click":
                    WebElement el = wait.until(ExpectedConditions.elementToBeClickable(getBy(locator)));
                    // JavaScript click to bypass overlays
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", el);
                    
                    if (!step.action.contains("silent")) {
                        Thread.sleep(2000); // Wait for sort/hide transition
                        waitForGridData();
                        currentNode.pass("Clicked: " + locator, 
                            MediaEntityBuilder.createScreenCaptureFromPath(takeScreenshot(shotName)).build());
                    }
                    break;

                case "full_traversal":
                    currentNode.info("Starting Full Grid Traversal...");
                    while (true) {
                        try {
                            // Locate the 'Next' button in the MUI pagination footer
                            WebElement nextBtn = driver.findElement(By.xpath("//button[@title='Go to next page']"));
                            
                            // Check if the button is disabled (last page reached)
                            if (!nextBtn.isEnabled() || nextBtn.getAttribute("disabled") != null) {
                                break;
                            }
                            
                            nextBtn.click();
                            Thread.sleep(1500); // Wait for the new rows to render
                        } catch (Exception e) {
                            // Exit loop if button is not found or other error occurs
                            break; 
                        }
                    }
                    // Snap the final screenshot of the last page
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
            // Wait for MUI rows to render
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