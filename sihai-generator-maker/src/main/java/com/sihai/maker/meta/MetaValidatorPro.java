package com.sihai.maker.meta;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.sihai.maker.meta.enums.FileConfigValuesEnum;
import com.sihai.maker.meta.enums.ModelTypeEnum;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MetaValidatorPro {

    // 定义常量
    private static final String DEFAULT_NAME = "my-generator";
    private static final String DEFAULT_DESCRIPTION = "模版代码生成器";
    private static final String DEFAULT_BASE_PACKAGE = "com.sihai";
    private static final String DEFAULT_VERSION = "1.0";
    private static final String DEFAULT_AUTHOR = "sihai";
    private static final String DEFAULT_CREATE_TIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String DEFAULT_FILE_CONFIG_TYPE = "dir";
    private static final String DEFAULT_OUTPUT_ROOT_PATH = "generated";
    private static final String DEFAULT_INPUT_ROOT_PATH_TEMPLATE = ".source/";
    private static final String GROUP_KEY = "group";

    /**
     * 验证并填充元信息及默认值
     */
    public static void doValidAndFillDefaultValue(Meta meta) {
        validateAndFillMetaRootInfo(meta);
        validateAndFillFileConfig(meta.getFileConfig());
        validateAndSetDefaultForModelConfig(meta.getModelConfig());
    }

    /**
     * 验证并填充元信息中的信息
     *
     * @param meta 元信息对象
     */
    private static void validateAndFillMetaRootInfo(Meta meta) {
        String name = meta.getName();
        String description = meta.getDescription();
        String basePackage = meta.getBasePackage();
        String version = meta.getVersion();
        String author = meta.getAuthor();
        String createTime = meta.getCreateTime();

        if (StrUtil.isBlank(name)) {
            meta.setName(DEFAULT_NAME);
        }

        if (StrUtil.isEmpty(description)) {
            meta.setDescription(DEFAULT_DESCRIPTION);
        }

        if (StrUtil.isBlank(basePackage)) {
            meta.setBasePackage(DEFAULT_BASE_PACKAGE);
        }

        if (StrUtil.isEmpty(version)) {
            meta.setVersion(DEFAULT_VERSION);
        }

        if (StrUtil.isEmpty(author)) {
            meta.setAuthor(DEFAULT_AUTHOR);
        }

        if (StrUtil.isEmpty(createTime)) {
            meta.setCreateTime(getCurrentTimeFormatted());
        }
    }

    /**
     * 验证并填充文件配置
     *
     * @param fileConfig 文件配置对象
     */
    private static void validateAndFillFileConfig(Meta.FileConfig fileConfig) {
        if (fileConfig != null) {
            validateSourceRootPath(fileConfig.getSourceRootPath());

            String defaultInputRootPath = DEFAULT_INPUT_ROOT_PATH_TEMPLATE +
                    FileUtil.getLastPathEle(Paths.get(fileConfig.getSourceRootPath())).getFileName().toString();
            fileConfig.setInputRootPath(StrUtil.isEmpty(fileConfig.getInputRootPath()) ? defaultInputRootPath : fileConfig.getInputRootPath());
            fileConfig.setOutputRootPath(StrUtil.isEmpty(fileConfig.getOutputRootPath()) ? DEFAULT_OUTPUT_ROOT_PATH : fileConfig.getOutputRootPath());
            fileConfig.setType(StrUtil.isEmpty(fileConfig.getType()) ? DEFAULT_FILE_CONFIG_TYPE : fileConfig.getType());

            fillFileInfoDefaults(fileConfig.getFiles());
        }
    }

    /**
     * 验证源根路径
     *
     * @param sourceRootPath 源根路径字符串
     */
    private static void validateSourceRootPath(String sourceRootPath) {
        if (StrUtil.isBlank(sourceRootPath)) {
            throw new MetaException("sourceRootPath 不能为空");
        }
    }

    /**
     * 填充文件信息的默认值
     *
     * @param fileInfoList 文件信息列表
     */
    private static void fillFileInfoDefaults(List<Meta.FileConfig.FileInfo> fileInfoList) {
        if (CollUtil.isNotEmpty(fileInfoList)) {
            fileInfoList.forEach(fileInfo -> {
                if (!GROUP_KEY.equals(fileInfo.getType())){
                    validateAndFillFileInfo(fileInfo);
                }
            });
        }
    }

    /**
     * 验证并填充文件信息
     *
     * @param fileInfo 文件信息对象
     */
    private static void validateAndFillFileInfo(Meta.FileConfig.FileInfo fileInfo) {
        validateInputPath(fileInfo.getInputPath());

        fileInfo.setOutputPath(StrUtil.isEmpty(fileInfo.getOutputPath()) ? fileInfo.getInputPath() : fileInfo.getOutputPath());
        fileInfo.setType(StrUtil.isBlank(fileInfo.getType()) ? determineFileType(fileInfo.getInputPath()) : fileInfo.getType());
        fileInfo.setGenerateType(determineGenerateType(fileInfo.getInputPath()));
    }

    /**
     * 验证输入路径
     *
     * @param inputPath 输入路径字符串
     */
    private static void validateInputPath(String inputPath) {
        if (StrUtil.isBlank(inputPath)) {
            throw new MetaException("inputPath 不能为空");
        }
    }

    /**
     * 根据输入路径确定文件类型
     *
     * @param inputPath 输入路径字符串
     * @return 文件类型，可能是 "file" 或 "dir"
     */
    private static String determineFileType(String inputPath) {
        if (StrUtil.isBlank(FileUtil.getSuffix(inputPath))) {
            return DEFAULT_FILE_CONFIG_TYPE;
        } else {
            return "file";
        }
    }

    /**
     * 根据输入路径确定生成类型
     *
     * @param inputPath 输入路径字符串
     * @return 生成类型，可能是 "dynamic" 或 "static"
     */
    private static String determineGenerateType(String inputPath) {
        return inputPath.endsWith(".ftl") ? "dynamic" : "static";
    }

    /**
     * 获取当前时间并格式化为指定的字符串格式
     *
     * @return 当前时间的字符串表示，格式为 "yyyy-MM-dd HH:mm:ss"
     */
    private static String getCurrentTimeFormatted() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DEFAULT_CREATE_TIME_FORMAT);
        return LocalDateTime.now().format(formatter);
    }

    /**
     * 校验并设置模型配置的默认值
     * @param modelConfig 元信息对象
     */
    public static void validateAndSetDefaultForModelConfig(Meta.ModelConfig modelConfig) {
        if (modelConfig == null) {
            return; // 提前返回，避免不必要的嵌套
        }

        List<Meta.ModelConfig.ModelInfo> modelInfoList = modelConfig.getModels();
        if (CollUtil.isEmpty(modelInfoList)) {
            return;
        }

        // 防止并发修改异常，做一次浅拷贝
        for (Meta.ModelConfig.ModelInfo modelInfo : new ArrayList<>(modelInfoList)) {
            // 如果 GroupKey 不为空，跳出循环
            try {
                if (StrUtil.isNotEmpty(modelInfo.getGroupKey())) {
                    // 优化字符串拼接性能
                    modelInfo.setAllArgsStr(buildAllArgsStr(modelInfo));
                    continue;
                }
                validateFieldName(modelInfo);
                setDefaultTypeIfEmpty(modelInfo);
            } catch (Exception e) { // 捕获并处理可能的异常，确保方法健壮性
                // 日志记录或其他错误处理逻辑
                System.err.println("Error processing model info: " + e.getMessage());
            }
        }
    }

    /**
     * 验证模型信息的字段名，如果为空则抛出异常
     *
     * @param modelInfo 要验证的模型信息
     * @throws MetaException 当字段名为空时抛出异常
     */
    private static void validateFieldName(Meta.ModelConfig.ModelInfo modelInfo) {
        if (StrUtil.isBlank(modelInfo.getFieldName())) {
            throw new MetaException("fieldName 不可为空");
        }
    }

    /**
     * 如果模型信息的类型为空，设置默认类型为 "String"
     *
     * @param modelInfo 要设置默认类型的模型信息
     */
    private static void setDefaultTypeIfEmpty(Meta.ModelConfig.ModelInfo modelInfo) {
        if (StrUtil.isEmpty(modelInfo.getType())) {
            modelInfo.setType(ModelTypeEnum.STRING.getValue());
        }
    }

    /**
     * 生成中间参数字符串
     * @param modelInfo 模型信息
     * @return 参数字符串
     */
    private static String buildAllArgsStr(Meta.ModelConfig.ModelInfo modelInfo) {
        // 生成中间参数
        String allArgsStr = modelInfo.getModels().stream()
                .map(subModelInfo -> String.format("\"--%s\"", subModelInfo.getFieldName()))
                .collect(Collectors.joining(", "));
        modelInfo.setAllArgsStr(allArgsStr);
        return allArgsStr;
    }

}
