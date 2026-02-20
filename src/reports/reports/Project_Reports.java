package reports.reports;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import javax.swing.JOptionPane; 

import org.openqa.selenium.*;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

public class Project_Reports {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected JavascriptExecutor js;
    
    protected static ExtentReports extent;
    protected static String reportDir;
    protected ExtentTest test;

    public void setupSuite() {
        if (extent == null) {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            reportDir = System.getProperty("user.dir") + "/Reports/YAML_Execution_" + timestamp;
            new File(reportDir + "/screenshots").mkdirs();

            ExtentSparkReporter reporter = new ExtentSparkReporter(reportDir + "/Automation_Report.html");
            extent = new ExtentReports();
            extent.attachReporter(reporter);
        }
    }

    public void setupBrowser() {
        try {
            File localDriver = new File("msedgedriver.exe");
            if (localDriver.exists()) {
                System.setProperty("webdriver.edge.driver", localDriver.getAbsolutePath());
            }

            EdgeOptions options = new EdgeOptions();
            options.addArguments("--remote-allow-origins=*");
            options.addArguments("--start-maximized");

            driver = new EdgeDriver(options);
            wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            js = (JavascriptExecutor) driver;
        } catch (Exception e) {
            throw new RuntimeException("Browser setup failed", e);
        }
    }

    public void cleanup() {
        if (driver != null) driver.quit();
        if (extent != null) extent.flush();
    }

    public String takeScreenshot(String name) {
        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            String path = reportDir + "/screenshots/" + name + ".png";
            File src = ts.getScreenshotAs(OutputType.FILE);
            org.openqa.selenium.io.FileHandler.copy(src, new File(path));
            return path;
        } catch (IOException e) { return null; }
    }

    public void selectCustomDropdown(String optionText) {
        By dropdownOption = By.xpath("//div[contains(@class,'max-h-[300px]')]//button[span[text()='" + optionText + "']]");
        wait.until(ExpectedConditions.elementToBeClickable(dropdownOption)).click();
    }
}