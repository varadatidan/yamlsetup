package reports.reports;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;
import javax.swing.JOptionPane;
import org.openqa.selenium.*;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

public class Project_Reports {

    public WebDriver driver;
    protected WebDriverWait wait;
    protected JavascriptExecutor js;
    
    public static ExtentReports extent;
    public static String reportDir;
    protected ExtentTest test;

    public void setupSuite() {
        if (extent == null) {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            reportDir = System.getProperty("user.dir") + File.separator + "Reports" + File.separator + "Batch_" + timestamp;
            
            File ssFolder = new File(reportDir + File.separator + "screenshots");
            if (!ssFolder.exists()) ssFolder.mkdirs();

            ExtentSparkReporter reporter = new ExtentSparkReporter(reportDir + File.separator + "Automation_Report.html");
            extent = new ExtentReports();
            extent.attachReporter(reporter);
            System.out.println("📊 Report Initialized: " + reportDir);
        }
    }

    public void setupBrowser() {
        try {
            // Updated path to point inside the 'resources' folder
            String driverPath = System.getProperty("user.dir") + File.separator + "resources" + File.separator + "msedgedriver.exe";
            File driverFile = new File(driverPath);

            if (driverFile.exists()) {
                System.setProperty("webdriver.edge.driver", driverFile.getAbsolutePath());
                System.out.println("✅ Driver Found in resources: " + driverFile.getAbsolutePath());
            } else {
                // Detailed error message to help you debug the path
                String errorMsg = "msedgedriver.exe not found!\n" +
                                  "Looked in: " + driverPath + "\n\n" +
                                  "Please ensure the file is named 'msedgedriver.exe' and is inside the 'resources' folder.";
                JOptionPane.showMessageDialog(null, errorMsg, "Driver Error", JOptionPane.ERROR_MESSAGE);
                throw new RuntimeException("Driver missing.");
            }

            EdgeOptions options = new EdgeOptions();
            options.addArguments("--remote-allow-origins=*", "--start-maximized");

            driver = new EdgeDriver(options);
            wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            js = (JavascriptExecutor) driver;
            
        } catch (Exception e) {
            System.err.println("❌ Browser setup failed: " + e.getMessage());
            throw new RuntimeException("Browser setup failed", e);
        }
    }

    public String takeScreenshot(String name) {
        try {
            TakesScreenshot ts = (TakesScreenshot) driver;
            String fileName = name + ".png";
            String fullPath = reportDir + File.separator + "screenshots" + File.separator + fileName;
            File src = ts.getScreenshotAs(OutputType.FILE);
            org.openqa.selenium.io.FileHandler.copy(src, new File(fullPath));
            return "screenshots/" + fileName; // Relative path for report portability
        } catch (IOException e) { return ""; }
    }
}