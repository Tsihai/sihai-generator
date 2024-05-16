package com.sihai.generator;

import com.sihai.model.MainTemplateConfig;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * 动态文件生成器
 * @Author sihai
 * @Date 2024/1/11 19:00
 */
public class DynamicGenerator {

    public static void main(String[] args) throws IOException, TemplateException {

    }

    /**
     * 生成文件
     * @param inputPath 模板文件输入路径
     * @param outputPath 输出路径
     * @param model 数据模型
     * @throws IOException
     * @throws TemplateException
     */
    public static void doGenerate(String inputPath, String outputPath, Object model) throws IOException, TemplateException {
        // 创建模板版本{
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);

        // 获取模板路径
        String projectPath = System.getProperty("user.dir") + File.separator + "sihai-generator-basic";
        File parentPath = new File(projectPath);
        File file = new File(parentPath, "src/main/resources/templates");

        // 设置模板路径
        cfg.setDirectoryForTemplateLoading(file);
        // 设置模板文件使用的字符集
        cfg.setDefaultEncoding("UTF-8");
        // 设置默认生成的数字格式
        cfg.setNumberFormat("0.######");

        // 创建模板对象，加载指定模板文件
        Template template = cfg.getTemplate("MainTemplate.java.ftl");

        // 设置数据模型
        MainTemplateConfig mainTemplateConfig = new MainTemplateConfig();
        // 是否开启循环
        mainTemplateConfig.setLoop(true);


        // 文件输出
        Writer out = new FileWriter("MainTemplate.java");

        template.process(mainTemplateConfig, out);

        out.close();
    }
}
