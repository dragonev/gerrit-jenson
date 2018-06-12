package com.dragon.gerrit.jenson.data.entity;

import java.util.ArrayList;
import java.util.List;

public class ScrReport {
	private String version;
	private List<ScrIssue> issues;
	
	public ScrReport() {
		this.issues = new ArrayList<ScrIssue>();
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public List<ScrIssue> getIssues() {
		return issues;
	}

	public void setIssues(List<ScrIssue> issues) {
		this.issues = issues;
	}
}
