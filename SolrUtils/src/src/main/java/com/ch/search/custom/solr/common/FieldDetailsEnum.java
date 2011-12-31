package com.ch.search.custom.solr.common;

import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.MapFieldSelector;

public enum FieldDetailsEnum {
		
	DEV_ID("DEV_ID", "DEV_NAME"),
	MSG_ID("MSG_ID", "MSG_NAME");
	
	private String idField;
	
	private String nameField;
	
	private FieldSelector fieldSelector;
	
	private FieldDetailsEnum(String idField, String nameField) {
		this.idField = idField;
		this.nameField = nameField;
		this.fieldSelector =  new MapFieldSelector(new String[] { nameField });
	}

	public String getIdField() {
		return idField;
	}

	public String getNameField() {
		return nameField;
	}

	public FieldSelector getFieldSelector() {
		return fieldSelector;
	}
	
	public static FieldDetailsEnum getEnumByKey(String key){
		for(FieldDetailsEnum e : values()){
			if(e.getIdField().equals(key)){
					return e;
			}
		}
		return null;
	}
}