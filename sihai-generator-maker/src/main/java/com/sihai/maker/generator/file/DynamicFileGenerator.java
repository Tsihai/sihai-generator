package com.sihai.maker.generator.file;

import cn.hutool.core.io.FileUtil;
import com.sihai.maker.model.DataModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 动态文件生成器
 * @Author sihai
 * @Date 2024/1/11 19:00
 */
public class DynamicFileGenerator {

    /**
     * 生成文件
     * @param inputPath 模板文件输入路径
     * @param outputPath 输出路径
     * @param model 数据模型
     * @throws IOException
     * @throws TemplateException
     */
    public static void doGenerate(String inputPath, String outputPath, Object model) throws IOException, TemplateException {
        // 创建 Configuration 模板版本
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);

        // 设置模板文件所在路径
        File templateDir = new File(inputPath).getParentFile();
        cfg.setDirectoryForTemplateLoading(templateDir);
        // 设置模板文件使用的字符集
        cfg.setDefaultEncoding("UTF-8");

        // 创建模板对象，加载指定模板文件
        String templateName = new File(inputPath).getName();
        Template template = cfg.getTemplate(templateName);

        // 判断文件是否存在
        if (!FileUtil.exist(outputPath)) {
            FileUtil.touch(outputPath);
        }

        // 文件生成
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(Paths.get(outputPath)), StandardCharsets.UTF_8));

        template.process(model, out);

        // 生成文件后关闭
        out.close();
    }
}
