package com.dragon.gerrit.jenson.util;

import java.text.SimpleDateFormat;
import java.util.Date;

import hudson.model.BuildListener;

public class Logger {
	
	private static final String MSG_INFO = "infor";
	private static final String MSG_DEBG = "debug";
	private static final String MSG_WARN = "warnn";
	private static final String MSG_ERRO = "error";
	private static final String MSG_FTAL = "fatal";
	private static final String TAG = "marok";
	private BuildListener listener = null;
	
	public Logger(BuildListener listener) {
		this.listener = listener;
	}

	private void formatOutput(String level, String tag, String msg) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss");
		if (tag.length() > 0) {
			listener.getLogger().printf("[%s %s %s] %s\n", df.format(new Date()), level, tag, msg);
		} else {
			listener.getLogger().printf("[%s %s %s] %s\n", df.format(new Date()), level, TAG, msg);
		}
	}
	
	public void i(String msg) {
		formatOutput(MSG_INFO, TAG, msg);
	}
	
	public void d(String msg) {
		formatOutput(MSG_DEBG, TAG, msg);
	}
	
	public void w(String msg) {
		formatOutput(MSG_WARN, TAG, msg);
	}
	
	public void e(String msg) {
		formatOutput(MSG_ERRO, TAG, msg);
	}
	
	public void f(String msg) {
		formatOutput(MSG_FTAL, TAG, msg);
	}
	
	public void i(String tag, String msg) {
		formatOutput(MSG_INFO, tag, msg);
	}
	
	public void d(String tag, String msg) {
		formatOutput(MSG_DEBG, tag, msg);
	}
	
	public void w(String tag, String msg) {
		formatOutput(MSG_WARN, tag, msg);
	}
	
	public void e(String tag, String msg) {
		formatOutput(MSG_ERRO, tag, msg);
	}
	
	public void f(String tag, String msg) {
		formatOutput(MSG_FTAL, tag, msg);
	}
}
