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
     * 作者
     */
    private String author = "sihai";

    /**
     * 输出信息提示
     */
    private String outputText = "sum = ";

    /**
     * 是否循环
     */
    private boolean loop;
}
