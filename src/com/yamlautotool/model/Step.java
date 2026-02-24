package com.yamlautotool.model;

public class Step {
    public String action;
    public String locator;
    public String value;
    public String url;
    public String expected; // Added to handle the "expected: exists" key
    public boolean clear_before;
    public boolean send_enter;
	public Step[] steps;
}