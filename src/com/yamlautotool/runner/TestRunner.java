package com.yamlautotool.runner;

import reports.reports.Project_Reports;

import com.aventstack.extentreports.MediaEntityBuilder;
import com.yamlautotool.model.*;
import com.yamlautotool.utils.YamlReader;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import java.util.Map;

public class TestRunner extends Project_Reports {

	public static void main(String[] args) {
	    try {
	        TestRunner runner = new TestRunner();
	        
//	         Run the Search Test first
	        runner.executeYaml("companies_count.yaml");
	        
	        // Run the Grid UI Test second
	        runner.executeYaml("companies_grid.yaml");
	        
	    } catch (Exception e) {
	        System.err.println("Execution failed: " + e.getMessage());
	        e.printStackTrace();
	    }
	}

    public void executeYaml(String yamlFile) throws Exception {
        setupSuite();  
        setupBrowser(); 
        
        try {
            TestCase testCase = YamlReader.loadTestCase(yamlFile);
            if (testCase == null) {
                throw new RuntimeException("Could not load YAML: " + yamlFile);
            }
            
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
                for (Step subStep : loopStep.steps) {
                    executeSingleStep(subStep, dataRow, ts);
                }
            }
        }
    }

    private void executeSingleStep(Step step, Map<String, String> dataRow, String ts) {
        String val = (step.value != null) ? step.value : "";
        String loc = (step.locator != null) ? step.locator : "";

        if (dataRow != null) {
            for (Map.Entry<String, String> entry : dataRow.entrySet()) {
                String target = "${" + entry.getKey() + "}";
                val = val.replace(target, entry.getValue());
                loc = loc.replace(target, entry.getValue());
            }
        }
        
        val = val.replace("${timestamp}", ts);
        loc = loc.replace("${timestamp}", ts);

        performAction(step, loc, val, ts);
    }

    private void performAction(Step step, String locator, String value, String ts) {
        try {
            switch (step.action.toLowerCase()) {
                case "navigate":
                    // PRIORITY: Use step.url if provided, otherwise fallback to value
                    String targetUrl = (step.url != null && !step.url.isEmpty()) ? step.url : value;
                    driver.get(targetUrl);
                    test.pass("Navigated to: " + targetUrl);
                    break;

                case "input":
                    WebElement input = wait.until(ExpectedConditions.visibilityOfElementLocated(getBy(locator)));
                    if (step.clear_before) input.clear();
                    input.sendKeys(value);
                    if (step.send_enter) input.sendKeys(Keys.ENTER);
                    test.pass("Entered: " + value);
                    break;

                case "mui_dropdown":
                    selectCustomDropdown(value); 
                    test.pass("Selected: " + value);
                    break;

                case "screenshot":
                    String path = takeScreenshot(value + "_" + ts);
                    test.pass("Screenshot captured", com.aventstack.extentreports.MediaEntityBuilder.createScreenCaptureFromPath(path).build());
                    break;

                case "wait":
                    Thread.sleep(Integer.parseInt(value));
                    break;
                    
                case "click":
                    WebElement element = wait.until(ExpectedConditions.elementToBeClickable(getBy(locator)));
                    // Use JavascriptExecutor for clicks to avoid 'ElementClickIntercepted' errors on MUI headers
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                    test.pass("Clicked element: " + locator);
                    break;
                case "refresh":
                    driver.navigate().refresh();
                    test.info("Browser page refreshed.");
                    // Optional: Add a small wait after refresh to ensure page stability
                    Thread.sleep(2000); 
                    break;
                case "hover":
                    WebElement header = wait.until(ExpectedConditions.visibilityOfElementLocated(getBy(locator)));
                    org.openqa.selenium.interactions.Actions actions = new org.openqa.selenium.interactions.Actions(driver);
                    actions.moveToElement(header).perform();
                    test.info("Hovered over: " + locator);
                    break;
                case "full_traversal":
                    test.info("<b>Starting Full Pagination Traversal</b>");
                    int pageCount = 1;
                    while (true) {
                        WebElement nextBtn = driver.findElement(By.xpath("//button[@title='Go to next page']"));
                        
                        // If button is disabled, we are on the last page
                        if (!nextBtn.isEnabled() || nextBtn.getAttribute("disabled") != null) {
                            String label = driver.findElement(By.xpath("//p[contains(@class, 'MuiTablePagination-displayedRows')]")).getText();
                            String pathEnd = takeScreenshot("Pagination_LastPage");
                            test.pass("Reached Last Page. Footer: " + label, 
                                MediaEntityBuilder.createScreenCaptureFromPath(pathEnd).build());
                            break;
                        }

                        nextBtn.click();
                        pageCount++;
                        Thread.sleep(2500); // Wait for grid to render
                    }
                    test.info("Traversed " + pageCount + " pages.");
                    break;
            }
        } catch (Exception e) {
            String errImg = takeScreenshot("Error_" + ts);
            test.fail("Failed: " + e.getMessage(), com.aventstack.extentreports.MediaEntityBuilder.createScreenCaptureFromPath(errImg).build());
        }
    }

    private By getBy(String locator) {
        String selector = locator.trim();
        if (selector.startsWith("xpath=")) return By.xpath(selector.substring(6));
        if (selector.startsWith("css=")) return By.cssSelector(selector.substring(4));
        return By.id(selector);
    }
}