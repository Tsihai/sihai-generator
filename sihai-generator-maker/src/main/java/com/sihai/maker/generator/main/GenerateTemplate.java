package com.sihai.maker.generator.main;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.sihai.maker.generator.JarGenerator;
import com.sihai.maker.generator.ScriptGenerator;
import com.sihai.maker.generator.file.DynamicFileGenerator;
import com.sihai.maker.meta.Meta;
import com.sihai.maker.meta.MetaManager;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 核心生成器
 */
public abstract class GenerateTemplate {

    public void doGenerate() throws IOException, InterruptedException, TemplateException {
        Meta meta = MetaManager.getMetaObject();
        // 输出的根目录
        String projectPath = System.getProperty("user.dir");
        // 输出的路径
        String outputPath = projectPath + File.separator + "generated" + File.separator + meta.getName();
        // 执行重写生成代码
        doGenerate(meta, outputPath);
    }

    /**
     * 重改生成代码
     */
    public void doGenerate(Meta meta, String outputPath) throws TemplateException, IOException, InterruptedException {
        ensureOutputDirectory(outputPath);

        // 将原模版文件复制到生成目录下
        String sourceRootPath = meta.getFileConfig().getSourceRootPath();
        copySourceFiles(sourceRootPath, outputPath + File.separator + ".source");

        // 读取 resources 目录`
        String inputResourcePath = "";

        // Java 包的基础路径
        String outputBasePackagePath = outputPath + File.separator + "src/main/java/" + StrUtil.join("/", StrUtil.split(meta.getBasePackage(), "."));

        // 动态生成文件
        List<String> templates = Arrays.asList(
                "java/model/DataModel.java.ftl",
                "java/cli/command/GenerateCommand.java.ftl",
                "java/cli/command/JsonGenerateCommand.java.ftl",
                "java/cli/command/ListCommand.java.ftl",
                "java/cli/command/ConfigCommand.java.ftl",
                "java/cli/CommandExecutor.java.ftl",
                "java/Main.java.ftl",
                "java/generator/DynamicGenerator.java.ftl",
                "java/generator/StaticGenerator.java.ftl",
                "java/generator/MainGenerator.java.ftl"

        );
        for (String template : templates) {
            generateFile(inputResourcePath, outputBasePackagePath, template, meta);
        }

        // 生成 pom.xml 文件
        String inputFilePath = inputResourcePath + File.separator + "templates/pom.xml.ftl";
        String outputFilePath = outputPath + File.separator + "pom.xml";
        DynamicFileGenerator.doGenerate(inputFilePath, outputFilePath, meta);


        // 构建 jar 包
        JarGenerator.doGenerate(outputPath);
        System.out.println("构建 jar 包完毕" + "\n");

        // 封装脚本
        String shellOutputPath = outputPath + File.separator + "generator";
        String jarName = String.format("%s-%s-jar-with-dependencies.jar", meta.getName(), meta.getVersion());
        String jarPath = "target/" + jarName;
        ScriptGenerator.doGenerate(shellOutputPath, jarPath);
        System.out.println("封装脚本完毕" + "\n");

        // 生成精简版产物包
        String distOutputPath = outputPath + "-dist";
        createDistPackage(outputPath, jarPath, shellOutputPath, distOutputPath, sourceRootPath);

        System.out.println("生成完毕");
    }

    /**
     * 判断输出目录是否存在，不存在则创建
     * @param outputPath
     */
    protected void ensureOutputDirectory(String outputPath) {
        if (!FileUtil.exist(outputPath)) {
            FileUtil.mkdir(outputPath);
        }
    }

    /**
     * 将原模版文件复制到生成目录下
     * @param sourceRootPath
     * @param destPath
     */
    protected void copySourceFiles(String sourceRootPath, String destPath) {
        FileUtil.copy(sourceRootPath, destPath, true);
    }

    /**
     * 生成文件
     * @param inputResourcePath
     * @param outputBasePackagePath
     * @param template
     * @param meta
     */
    protected void generateFile(String inputResourcePath, String outputBasePackagePath, String template, Meta meta) throws TemplateException, IOException {
        String templateWithoutJava = template.substring(template.indexOf("/") + 1);
        String inputFilePath = inputResourcePath + File.separator + "templates" + File.separator + template;
        String outputFilePath = outputBasePackagePath + File.separator + templateWithoutJava.replaceAll("\\.ftl$", "");
        DynamicFileGenerator.doGenerate(inputFilePath, outputFilePath, meta);
    }

    /**
     * 创建精简版产物包
     * @param outputPath
     * @param jarPath
     * @param shellOutputPath
     * @param distOutputPath
     */
    protected String createDistPackage(String outputPath, String jarPath, String shellOutputPath, String distOutputPath, String sourceRootPath) {
        String sourcePath = outputPath + File.separator + ".source";
        FileUtil.mkdir(distOutputPath + File.separator + "target");
        FileUtil.copy(new File(outputPath, jarPath), new File(distOutputPath, jarPath), true);
        FileUtil.copy(new File(shellOutputPath), new File(distOutputPath), true);
        FileUtil.copy(new File(shellOutputPath + ".bat"), new File(distOutputPath), true);
        FileUtil.copy(new File(sourcePath), new File(distOutputPath), true);
        return distOutputPath;
    }

    /**
     * 制作压缩包
     *
     * @param outputPath
     * @return 压缩包路径
     */
    protected String buildZip(String outputPath) {
        String zipPath = outputPath + ".zip";
        ZipUtil.zip(outputPath, zipPath);
        return zipPath;
    }

}