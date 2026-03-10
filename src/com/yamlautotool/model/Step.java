package com.yamlautotool.model;

import java.util.List;

public class Step {
    public String name;
    public String action;
    public String locator;
    public String value;
    public String url;
    public boolean send_enter;
    public boolean clear_before; // <--- ADD THIS
    public List<Step> steps;      // <--- AND THIS (for data_loop)
    public String screenshot_prefix; // <--- AND THIS (for custom naming)
}