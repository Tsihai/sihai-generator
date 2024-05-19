package ${basePackage}.generator;

import cn.hutool.core.io.FileUtil;
import ${basePackage}.model.DataModel;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

/**
 * 动态文件生成器
 */
public class DynamicGenerator {

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
        // 设置默认生成的数字格式
        cfg.setNumberFormat("0.######");

        // 创建模板对象，加载指定模板文件
        String templateName = new File(inputPath).getName();
        Template template = cfg.getTemplate(templateName);

        // 判断文件是否存在
        if (!FileUtil.exist(outputPath)) {
            FileUtil.touch(outputPath);
        }

        // 文件生成
        Writer out = new FileWriter(outputPath);

        template.process(model, out);

        // 生成文件后关闭
        out.close();
    }
}
