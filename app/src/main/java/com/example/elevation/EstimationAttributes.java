package com.example.elevation;

public class EstimationAttributes {

    private String attributeKey;
    private String attributeValue;

    public EstimationAttributes(String attrKey, String attrValue) {
        this.attributeKey = attrKey;
        this.attributeValue = attrValue;
    }

    public String getAttributeKey() {
        return this.attributeKey;
    }

    public String getAttributeValue() {
        return this.attributeValue;
    }
}
