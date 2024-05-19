package com.sihai.maker.meta.enums;

/**
 * 元信息默认值枚举
 */
public enum MetaDefaultValuesEnum {

    NAME("名称","my-generator"),

    DESCRIPTION("描述","模版代码生成器"),

    BASE_PACKAGE("包","com.sihai"),

    VERSION("版本","1.0"),

    AUTHOR("作者","sihai"),

    CREATE_TIME_FORMAT("创建日期","yyyy-MM-dd HH:mm:ss");

    private final String text;

    private final String value;

    MetaDefaultValuesEnum(String text, String value) {
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
