package com.sihai.maker.meta.enums;

/**
 * 文件配置默认值枚举
 */
public enum FileConfigValuesEnum {

    DIR("目录","dir"),

    OUTPUT_ROOT_PATH("输出根目录","generated"),

    GROUP("文件组", "group"),

    INPUT_ROOT_PATH_TEMPLATE("输入根路径模板",".source");

    private final String text;

    private final String value;

    FileConfigValuesEnum(String text, String value) {
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
