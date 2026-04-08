package com.yamlautotool.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import com.yamlautotool.model.TestCase;

public class YamlReader {
	public static TestCase loadTestCase(String filePath) throws Exception {
	    Yaml yaml = new Yaml(); // Assuming you use SnakeYAML or YamlBeans
	    File file = new File(filePath);
	    
	    if (!file.exists()) {
	        System.out.println("System looking at: " + file.getAbsolutePath());
	        throw new Exception("File not found at path: " + filePath);
	    }

	    try (FileInputStream inputStream = new FileInputStream(file)) {
	        return yaml.loadAs(inputStream, TestCase.class);
	    } catch (Exception e) {
	        e.printStackTrace();
	        return null;
	    }
	
	}

	public static String getLocatorsAsContext() {
		// TODO Auto-generated method stub
		return null;
	}
	}