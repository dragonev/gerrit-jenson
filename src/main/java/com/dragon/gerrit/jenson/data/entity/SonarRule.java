package com.dragon.gerrit.jenson.data.entity;

import java.util.HashMap;
import java.util.Map;

public class SonarRule {
	
	private String version;
	private Map<String, String> rules;
	
	public SonarRule() {
		rules = new HashMap<String, String>();
	}

	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}

	public Map<String, String> getRules() {
		return rules;
	}

	public void setRules(Map<String, String> rules) {
		this.rules = rules;
	}
}
