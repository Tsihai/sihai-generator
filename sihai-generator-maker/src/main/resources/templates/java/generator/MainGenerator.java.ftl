package ${basePackage}.generator;

import freemarker.template.TemplateException;
import ${basePackage}.model.DataModel;

import java.io.File;
import java.io.IOException;

<#macro generateFile indent fileInfo>
${indent}inputPath = new File(inputRootPath, "${fileInfo.inputPath}").getAbsolutePath();
${indent}outputPath = new File(outputRootPath, "${fileInfo.outputPath}").getAbsolutePath();
<#if fileInfo.generateType == "static">
${indent}StaticGenerator.copyFilesByHutool(inputPath, outputPath);
<#else>
${indent}DynamicGenerator.doGenerate(inputPath, outputPath, model);
</#if>
</#macro>

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
    public static void doGenerate(DataModel model) throws TemplateException, IOException {

        // 绝对路径
        String inputRootPath = "${fileConfig.inputRootPath}";
        String outputRootPath = "${fileConfig.outputRootPath}";
        // 相对路径
        String inputPath;
        String outputPath;

    <#list modelConfig.models as modelInfo>
        <#if modelInfo.groupKey??>
        <#list modelInfo.models as subModelInfo>
        ${subModelInfo.type} ${subModelInfo.fieldName} = model.${modelInfo.groupKey}.${subModelInfo.fieldName};
        </#list>
        <#else>
        ${modelInfo.type} ${modelInfo.fieldName} = model.${modelInfo.fieldName};
        </#if>
    </#list>

    <#list fileConfig.files as fileInfo>
    <#if fileInfo.groupKey??>
        <#if fileConfig.condition??>
        if (${fileConfig.condition}) {
            <#list fileInfo.files as fileInfo>
            <@generateFile indent="                " fileInfo=fileInfo />
            </#list>
        }
        <#else>
        <#list fileInfo.files as fileInfo>
        <@generateFile indent="       " fileInfo=fileInfo />
        </#list>
        </#if>
        <#else>
        <#if fileInfo.condition??>
        if (${fileInfo.condition}) {
        <@generateFile indent="       " fileInfo=fileInfo />
        }
        <#else>
        <@generateFile indent="       " fileInfo=fileInfo />
        </#if>
        </#if>
    </#list>
    }
}
