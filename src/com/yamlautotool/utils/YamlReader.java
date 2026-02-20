package com.yamlautotool.utils;

import java.io.InputStream;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.constructor.Constructor;
import com.yamlautotool.model.TestCase;

public class YamlReader {
    public static TestCase loadTestCase(String fileName) {
        try {
            LoaderOptions options = new LoaderOptions();
            Constructor constructor = new Constructor(TestCase.class, options);
            Yaml yaml = new Yaml(constructor);

            InputStream inputStream = YamlReader.class
                .getClassLoader()
                .getResourceAsStream(fileName);

            if (inputStream == null) {
                System.err.println("File not found in resources: " + fileName);
                return null;
            }
            return yaml.load(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}