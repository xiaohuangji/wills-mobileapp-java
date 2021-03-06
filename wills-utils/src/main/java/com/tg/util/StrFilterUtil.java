package com.tg.util;

public class StrFilterUtil {

	/**
	 * 过滤以下特殊字符 + – && || ! ( ) { } [ ] ^ ” ~ * ? : \ /
	 * 
	 * @param input
	 * @return
	 */
	public static String queryFilter(String input, boolean filterNum) {
		if(input==null||input.isEmpty())
			return null;
		// StringBuffer sb = new StringBuffer();
		String regex;
		if (filterNum) {
			regex = "[+\\-&|!(){}\\[\\]^\"~*?%#@:(\\)/\\d]";// 将非字母字符都过滤成空格，然后按多字段匹配。
		} else {
			regex = "[+\\-&|!(){}\\[\\]^\"~*?%#@:(\\)/]";
		}
		/*
		 * Pattern pattern = Pattern.compile(regex); Matcher matcher =
		 * pattern.matcher(input); while(matcher.find()){
		 * matcher.appendReplacement(sb, "\\\\"+matcher.group()); }
		 * matcher.appendTail(sb);
		 */
		return input.replaceAll(regex, "");
	}
	
	//判断一串字符串是否都为空格或者为空，empty
	public static boolean isBlank(String input){
		if(input==null||input.isEmpty())
			return true;
		for(int i=0;i<input.length();i++){
			if(input.charAt(i)!=32&&input.charAt(i)!=9){
				return false;
			}
		}
		return true;
	}

	public static void main(String[] args) {
		//System.out.println(StrFilterUtil.queryFilter("  fsdf%&1", true));
		System.out.println(StrFilterUtil.isBlank(null));
	}
}
