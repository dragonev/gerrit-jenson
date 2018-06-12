package com.dragon.gerrit.jenson.data.entity;

public class ScrIssue {
	private String file;
	private Integer line;
	private String id;
	private String severity;
	private String message;
	
	public ScrIssue() {}
	
	public ScrIssue(String file, Integer line, String id, String severity, String message) {
		this.file = file;
		this.line = line;
		this.id = id;
		this.severity = severity;
		this.message = message;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public Integer getLine() {
		return line;
	}

	public void setLine(Integer line) {
		this.line = line;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	@Override
	public String toString() {
		return "ScrIssue [file=" + file + ", line=" + line + ", id=" + id + ", severity=" + severity + ", message="
				+ message + "]";
	}
}
