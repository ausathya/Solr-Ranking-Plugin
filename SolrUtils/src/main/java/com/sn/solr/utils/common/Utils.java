package com.sn.solr.utils.common;

import java.text.DecimalFormat;

public class Utils {
	
	static final DecimalFormat NUMBER_FORMAT = new DecimalFormat("##.####");
	
	public static String getDiffTime(long startTime){
		return NUMBER_FORMAT.format((System.nanoTime()-startTime) * 0.000001) + " ms";
	}
	
	public static boolean isInteger(String str) {
		if (str == null) {
			return false;
		}
		int length = str.length();
		if (length == 0) {
			return false;
		}
		int i = 0;
		if (str.charAt(0) == '-') {
			if (length == 1) {
				return false;
			}
			i = 1;
		}
		for (; i < length; i++) {
			char c = str.charAt(i);
			if (c <= '/' || c >= ':') {
				return false;
			}
		}
		return true;
	}

}
