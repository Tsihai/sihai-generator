package com.sihai.maker.generator.main;

import freemarker.template.TemplateException;

import java.io.IOException;

/**
 * 核心生成器 (可拓展通用类)
 */
public class MainGenerator extends GenerateTemplate{

    @Override
    @Deprecated
    protected void createDistPackage(String outputPath, String jarPath, String shellOutputPath, String distOutputPath) {
        System.out.println("不生成 dist 文件了");
    }

}
