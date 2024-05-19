package com.sihai.generator;

import com.sihai.model.MainTemplateConfig;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;

/**
 * 核心生成器
 */
public class MainGenerator {

    /**
     * 生成
     *
     * @param model 数据模型
     * @throws TemplateException
     * @throws IOException
     */
    public static void doGenerate(Object model) throws TemplateException, IOException {

        // 绝对路径
        String inputRootPath = "D:\\Object\\generator\\sihai-generator\\sihai-generator-demo\\acm-template-pro";
        String outputRootPath = "D:\\Object\\generator\\sihai-generator";
        // 相对路径
        String inputPath;
        String outputPath;

        // 动态生成文件
        inputPath = new File(inputRootPath, "src/com/sihai/acm/MainTemplate.java.ftl").getAbsolutePath();
        outputPath = new File(outputRootPath, "src/com/sihai/acm/MainTemplate.java").getAbsolutePath();
        DynamicGenerator.doGenerate(inputPath, outputPath, model);

        // 静态生成文件
        inputPath = new File(inputRootPath, ".gitignore").getAbsolutePath();
        outputPath = new File(inputRootPath, ".gitignore").getAbsolutePath();
        StaticGenerator.copyFilesByRecursive(inputPath, outputPath);
        inputPath = new File(inputRootPath, "README.md").getAbsolutePath();
        outputPath = new File(inputRootPath, "README.md").getAbsolutePath();
        StaticGenerator.copyFilesByRecursive(inputPath, outputPath);
    }

    public static void main(String[] args) throws TemplateException, IOException {
        // 配置数据模型
        MainTemplateConfig mainTemplateConfig = new MainTemplateConfig();
        mainTemplateConfig.setAuthor("sihai");
        mainTemplateConfig.setLoop(false);
        mainTemplateConfig.setOutputText("求和结果：");
        // 生成代码
        doGenerate(mainTemplateConfig);
    }
}
