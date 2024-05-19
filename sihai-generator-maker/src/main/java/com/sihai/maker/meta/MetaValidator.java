package com.sihai.maker.meta;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

/**
 * 元信息验证器
 */
public class MetaValidator {

    /**
     * 验证元信息是否有效并填充默认值
     * @param meta
     */
    public static void doValidAndFillDefaultValue(Meta meta) {
        // 判断元信息是否为空
        if (meta == null) {
            throw new MetaException("元信息不能为空");
        }

        // 基础信息校验和默认值
        String name = meta.getName();
        String description = meta.getDescription();
        String basePackage = meta.getBasePackage();
        String version = meta.getVersion();
        String author = meta.getAuthor();
        String createTime = meta.getCreateTime();
        Meta.FileConfig fileConfig = meta.getFileConfig();

        if (StrUtil.isBlank(name)) {
            name = "my-generator";
            meta.setName(name);
        }

        if (StrUtil.isEmpty(description)) {
            description = "模版代码生成器";
            meta.setDescription(description);
        }

        if (StrUtil.isBlank(basePackage)) {
            basePackage = "com.sihai";
            meta.setBasePackage(basePackage);
        }

        if (StrUtil.isEmpty(version)) {
            version = "1.0";
            meta.setVersion(version);
        }

        if (StrUtil.isEmpty(author)) {
            author = "sihai";
            meta.setAuthor(author);
        }

        if (StrUtil.isEmpty(createTime)) {
            createTime = DateUtil.now();
            meta.setCreateTime(createTime);
        }


        // 文件配置校验和默认值
        if (fileConfig != null) {
            // fileConfig 验证
            String inputRootPath = fileConfig.getInputRootPath();
            String outputRootPath = fileConfig.getOutputRootPath();
            String sourceRootPath = fileConfig.getSourceRootPath();

            // 由于 sourceRootPath 为必填项，首先验证
            if (StrUtil.isBlank(sourceRootPath)) {
                throw new MetaException("sourceRootPath 不能为空");
            }

            String defaultInputRootPath = ".source" + File.separator +
                    FileUtil.getLastPathEle(Paths.get(sourceRootPath)).getFileName().toString();
            if (StrUtil.isEmpty(inputRootPath)) {
                fileConfig.setInputRootPath(defaultInputRootPath);
            }

            String defaultOutputRootPath = "generated";
            if (StrUtil.isEmpty(outputRootPath)) {
                fileConfig.setOutputRootPath(defaultOutputRootPath);
            }

            String fileConfigType = fileConfig.getType();
            String defaultType = "dir";
            if (StrUtil.isEmpty(fileConfigType)) {
                fileConfig.setType(defaultType);
            }

            // files 验证
            List<Meta.FileConfig.FileInfo> fileInfoList = fileConfig.getFiles();
            if (CollUtil.isNotEmpty(fileInfoList)) {
                for (Meta.FileConfig.FileInfo fileInfo : fileInfoList) {
                    String inputPath = fileInfo.getInputPath();
                    if (StrUtil.isBlank(inputPath)) {
                        throw new MetaException("inputPath 不能为空");
                    }

                    String outputPath = fileInfo.getOutputPath();
                    if (StrUtil.isEmpty(outputPath)) {
                        fileInfo.setOutputPath(inputPath);
                    }

                    String type = fileInfo.getType();
                    if (StrUtil.isBlank(type)) {
                        // 无文件后缀判断
                        if (StrUtil.isBlank(FileUtil.getSuffix(inputPath))) {
                            fileInfo.setType("dir");
                        } else {
                            fileInfo.setType("file");
                        }
                    }

                    String generateType = fileInfo.getGenerateType();
                    // 判断 generateType 文件后缀是否为 .ftl, 是的话为 Dynamic, 否则为 static
                    if (StrUtil.isBlank(generateType)) {
                        if (inputPath.endsWith(".ftl")) {
                            fileInfo.setGenerateType("dynamic");
                        } else {
                            fileInfo.setGenerateType("static");
                        }
                    }

                }
            }

        }


        // 模型配置校验和默认值
        Meta.ModelConfig modelConfig = meta.getModelConfig();
        if (modelConfig != null) {
            List<Meta.ModelConfig.ModelInfo> modelInfoList = modelConfig.getModels();
            if (CollUtil.isNotEmpty(modelInfoList)) {
                for (Meta.ModelConfig.ModelInfo modelInfo : modelInfoList) {
                    String fieldName = modelInfo.getFieldName();
                    if (StrUtil.isBlank(fieldName)) {
                        throw new MetaException("fieldName 不能为空");
                    }

                    String modelInfoType = modelInfo.getType();
                    if (StrUtil.isEmpty(modelInfoType)) {
                        modelInfo.setType("String");
                    }
                }
            }
        }


    }
}
