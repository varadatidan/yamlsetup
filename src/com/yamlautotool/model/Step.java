package com.yamlautotool.model;

import java.util.List;

public class Step {
    public String action;
    public String locator;
    public String url;           // For navigate actions
    public String value;
    public String screenshot_prefix;
    public boolean clear_before; // For clearing input fields
    public boolean send_enter;   // For submitting after input
    public List<Step> steps;     // For nested data_loop steps

    public List<Step> getSteps() { return steps; }
    public void setSteps(List<Step> steps) { this.steps = steps; }
}