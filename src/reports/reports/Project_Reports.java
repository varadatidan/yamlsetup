package reports.reports;

import java.io.File;
import java.time.Duration;
import java.util.function.Consumer; // Added for logging
import org.openqa.selenium.*;
import org.openqa.selenium.edge.*;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

public class Project_Reports {

    public WebDriver driver;
    public WebDriverWait wait;
    public JavascriptExecutor js;
    public ExtentReports extent;
    public static String reportDir;
    protected ExtentTest test;
    public boolean isHeadless = false;

    // --- NEW: UI Logger Callback ---
    public Consumer<String> uiLogger;

    public void log(String message) {
        String time = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        String msg = "[" + time + "] " + message;
        System.out.println(msg); 
        if (uiLogger != null) {
            uiLogger.accept(msg + "\n");
        }
    }

    public void setupSuite() {
        if (extent == null) {
            String ts = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date());
            reportDir = System.getProperty("user.dir") + File.separator + "Reports" + File.separator + "Batch_" + ts;
            new File(reportDir + File.separator + "screenshots").mkdirs();
            ExtentSparkReporter reporter = new ExtentSparkReporter(reportDir + File.separator + "Automation_Report.html");
            extent = new ExtentReports();
            extent.attachReporter(reporter);
        }
    }

    public void setupBrowser() {
        String driverPath = System.getProperty("user.dir") + File.separator + "resources" + File.separator + "msedgedriver.exe";
        System.setProperty("webdriver.edge.driver", driverPath);
        EdgeOptions options = new EdgeOptions();
        options.addArguments("--remote-allow-origins=*", "--start-maximized");
        if (isHeadless) {
            options.addArguments("--headless=new", "--disable-gpu", "--window-size=1920,1080");
        }
        options.setBinary("C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe");
        driver = new EdgeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        js = (JavascriptExecutor) driver;
    }

    public String takeScreenshot(String name) {
        try {
            String path = reportDir + File.separator + "screenshots" + File.separator + name + ".png";
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            org.openqa.selenium.io.FileHandler.copy(src, new File(path));
            return "screenshots/" + name + ".png";
        } catch (Exception e) { return ""; }
    }
    
    public void executeYaml(String path) throws Exception { }
}