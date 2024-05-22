package com.sihai.maker.meta.enums;

/**
 * 模型配置默认值枚举
 */
public enum ModelTypeEnum {

    STRING("模型配置默认类型","String");

    private final String text;

    private final String value;

    ModelTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    public String getText() {
        return text;
    }

    public String getValue() {
        return value;
    }
}
