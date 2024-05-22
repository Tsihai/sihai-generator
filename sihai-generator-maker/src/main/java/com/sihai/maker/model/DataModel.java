package com.sihai.maker.model;

import lombok.Data;

/**
 * 静态模板配置
 * @Author sihai
 * @Date 2024/1/9 0:07
 */
@Data
public class DataModel {

    /**
     * 是否生成 .gitignore
     */
    private boolean needGit = true;

    /**
     * 是否循环
     */
    private boolean loop;

    /**
     * 核心模版
     */
    public MainTemplate mainTemplate;

    @Data
    public class MainTemplate {

        public String author = "sihai";

        public String outputText = "sum = ";

    }
}
