package com.sihai.maker.template;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.sihai.maker.meta.Meta;
import com.sihai.maker.meta.enums.FileConfigValuesEnum;
import com.sihai.maker.template.Filter.FileFilter;
import com.sihai.maker.template.model.TemplateMakerConfig;
import com.sihai.maker.template.model.TemplateMakerFileConfig;
import com.sihai.maker.template.model.TemplateMakerModelConfig;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模版制作工具
 */
public class TemplateMakerPro {

    public static long makeTemplate(TemplateMakerConfig templateMakerConfig) {
        Long id = templateMakerConfig.getId();
        Meta meta = templateMakerConfig.getMeta();
        String originProjectPath = templateMakerConfig.getOriginProjectPath();
        TemplateMakerFileConfig templateMakerFileConfig = templateMakerConfig.getFileConfig();
        TemplateMakerModelConfig templateMakerModelConfig = templateMakerConfig.getModelConfig();

        return makeTemplate(meta, originProjectPath, templateMakerFileConfig, templateMakerModelConfig, id);
    }

    /**
     * 制作模版
     *
     * @param newMeta
     * @param originProjectPath
     * @param templateMakerFileConfig
     * @param templateMakerModelConfig
     * @param id
     * @return
     */
    public static long makeTemplate(Meta newMeta, String originProjectPath, TemplateMakerFileConfig templateMakerFileConfig, TemplateMakerModelConfig templateMakerModelConfig, Long id) {
        id = initId(id);

        // 创建临时工作空间
        String templatePath = createTempWorkspace(originProjectPath, id);

        // 获取转义后的源项目根路径，适配Windows系统
        String sourceRootPath = getEscapedSourceRootPath(originProjectPath, templatePath);

        // 处理文件信息
        List<Meta.FileConfig.FileInfo> newFileInfoList = processFileGroup(templateMakerFileConfig, templateMakerModelConfig, sourceRootPath);

        // 处理模型信息
        List<Meta.ModelConfig.ModelInfo> newModelInfoList = processModelGroup(templateMakerModelConfig, convertModelInfoList(templateMakerModelConfig));

        // 生成配置文件
        Result result = generateConfig(newMeta, sourceRootPath, newFileInfoList, newModelInfoList, templatePath);

        // 输出元信息文件
        FileUtil.writeUtf8String(JSONUtil.toJsonPrettyStr(result.newMeta), result.metaOutputPath);

        return id;
    }

    /**
     * 制作模版文件
     * @param templateMakerModelConfig
     * @param sourceRootPath
     * @param inputFile
     * @return
     */
    private static Meta.FileConfig.FileInfo makeFileTemplate(TemplateMakerModelConfig templateMakerModelConfig, String sourceRootPath, File inputFile) {
        // 获取输入文件的相对路径
        String fileInputPath = getRelativePath(inputFile, sourceRootPath);
        String fileOutputPath = fileInputPath + ".ftl";
        // 获取输入文件的绝对路径（用于后续操作）
        String fileInputAbsolutePath = inputFile.getAbsolutePath().replace("\\", "/");
        // 使用字符串替换，生成模版文件
        String fileOutputAbsolutePath = fileInputAbsolutePath + ".ftl";
        // 是否存在模板文件
        boolean hasTemplateFile = FileUtil.exist(fileOutputAbsolutePath);

        // 读取文件内容，判断：如果已有模版文件，则在原有模版文件中挖坑
        String fileContent = getFileContent(fileOutputAbsolutePath, fileInputAbsolutePath, hasTemplateFile);

        // 替换模板字段
        String newFileContent = replaceTemplateFields(fileContent, templateMakerModelConfig);

        // 创建文件信息对象
        Meta.FileConfig.FileInfo fileInfo = createFileInfo(fileInputPath, fileOutputPath);

        // 判断是否和源文件相同，则不生成模版文件，静态生成
        // 是否修改文件内容
        boolean contentEquals = newFileContent.equals(fileContent);
        if (!hasTemplateFile) {
            if (newFileContent.equals(fileContent)) {
                fileInfo.setInputPath(fileInputPath);
                fileInfo.setGenerateType(FileConfigValuesEnum.STATIC.getValue());
            } else {
                // 输出模版文件
                FileUtil.writeUtf8String(newFileContent, fileOutputAbsolutePath);
            }
        } else if (!newFileContent.equals(fileContent)) {
            // 输出模版文件
            FileUtil.writeUtf8String(newFileContent, fileOutputAbsolutePath);
        }
        return fileInfo;
    }

    /**
     * 初始化 id
     *
     * @param id
     * @return
     */
    private static Long initId(Long id) {
        return id == null ? IdUtil.getSnowflakeNextId() : id;
    }

    /**
     * 创建临时工作空间
     * @param originProjectPath
     * @param id
     * @return
     */
    private static String createTempWorkspace(String originProjectPath, Long id) {
        String projectPath = System.getProperty("user.dir");
        String tempDirPath = projectPath + File.separator + ".temp";
        String templatePath = tempDirPath + File.separator + id;
        // 判断临时工作空间是否存在，不存在则创建
        if (!FileUtil.exist(templatePath)) {
            FileUtil.mkdir(templatePath);
            FileUtil.copy(originProjectPath, templatePath, true);
        }
        return templatePath;
    }

    /**
     * 获取转义后的源项目根路径，适配Windows系统
     *
     * @param originProjectPath
     * @param templatePath
     * @return
     */
    private static String getEscapedSourceRootPath(String originProjectPath, String templatePath) {
        File tempFile = new File(templatePath);
        templatePath = tempFile.getAbsolutePath();
        String sourceRootPath = FileUtil.loopFiles(new File(templatePath), 1, null)
                .stream()
                .filter(File::isDirectory)
                .findFirst()
                .orElseThrow(RuntimeException::new)
                .getAbsolutePath();
        sourceRootPath = sourceRootPath.replace("\\", "/");
        return sourceRootPath;
    }

    /**
     * 将模板模型配置转换为Meta接受的ModelInfo对象列表
     *
     * @param templateMakerModelConfig
     * @return
     */
    private static List<Meta.ModelConfig.ModelInfo> convertModelInfoList(TemplateMakerModelConfig templateMakerModelConfig) {
        // 本次新增的模型配置列表
        List<Meta.ModelConfig.ModelInfo> newModelInfoList = new ArrayList<>();
        if (templateMakerModelConfig == null) {
            return newModelInfoList;
        }
        List<TemplateMakerModelConfig.ModelInfoConfig> models = templateMakerModelConfig.getModels();
        if (CollUtil.isEmpty(models)) {
            return newModelInfoList;
        }
        return models.stream()
                .map(modelInfoConfig -> {
                    Meta.ModelConfig.ModelInfo modelInfo = new Meta.ModelConfig.ModelInfo();
                    BeanUtil.copyProperties(modelInfoConfig, modelInfo);
                    return modelInfo;
                })
                .collect(Collectors.toList());
    }

    /**
     * 处理模型分组逻辑，优化代码结构
     * 如果为模型组
     */
    private static List<Meta.ModelConfig.ModelInfo> processModelGroup(TemplateMakerModelConfig templateMakerModelConfig, List<Meta.ModelConfig.ModelInfo> inputModelInfoList) {
        List<Meta.ModelConfig.ModelInfo> newModelInfoList = new ArrayList<>();
        TemplateMakerModelConfig.ModelGroupConfig modelGroupConfig = templateMakerModelConfig.getModelGroupConfig();

        if (modelGroupConfig != null) {
            Meta.ModelConfig.ModelInfo groupModelInfo = new Meta.ModelConfig.ModelInfo();
            BeanUtil.copyProperties(modelGroupConfig, groupModelInfo);

            groupModelInfo.setModels(inputModelInfoList);
            newModelInfoList.add(groupModelInfo);
        } else {
            newModelInfoList.addAll(inputModelInfoList);
        }
        return newModelInfoList;
    }

    /**
     * 处理文件信息，包括过滤、分组 操作
     * @param templateMakerFileConfig
     * @param sourceRootPath
     * @return
     */
    private static List<Meta.FileConfig.FileInfo> processFileGroup(TemplateMakerFileConfig templateMakerFileConfig, TemplateMakerModelConfig templateMakerModelConfig, String sourceRootPath) {
        // 遍历输入文件
        List<Meta.FileConfig.FileInfo> newFileInfoList = new ArrayList<>();
        if (templateMakerFileConfig == null) {
            return newFileInfoList;
        }

        List<TemplateMakerFileConfig.FileInfoConfig> fileConfigInfoList = templateMakerFileConfig.getFiles();
        if (CollUtil.isEmpty(fileConfigInfoList)) {
            return newFileInfoList;
        }

        for (TemplateMakerFileConfig.FileInfoConfig fileInfoConfig : fileConfigInfoList) {
            String inputFilePath = fileInfoConfig.getPath();

            // 如果填的是相对路径，要改为绝对路径
            if (!inputFilePath.startsWith(sourceRootPath)) {
                inputFilePath = sourceRootPath + File.separator + inputFilePath;
            }

            // 获取过滤后的文件列表（不会存在目录）
            List<File> fileList = FileFilter.doFilter(inputFilePath, fileInfoConfig.getFilterConfigList());
            fileList = fileList.stream()
                    .filter(file -> !file.getAbsolutePath().endsWith(".ftl"))
                    .collect(Collectors.toList());
            for (File file : fileList) {
                Meta.FileConfig.FileInfo fileInfo = makeFileTemplate(templateMakerModelConfig, sourceRootPath, file);
                newFileInfoList.add(fileInfo);
            }
        }

        // 如果是文件组
        TemplateMakerFileConfig.FileGroupConfig fileGroupConfig = templateMakerFileConfig.getFileGroupConfig();
        if (fileGroupConfig != null) {
            String condition = fileGroupConfig.getCondition();
            String groupKey = fileGroupConfig.getGroupKey();
            String groupName = fileGroupConfig.getGroupName();

            Meta.FileConfig.FileInfo groupFileInfo = new Meta.FileConfig.FileInfo();
            groupFileInfo.setCondition(condition);
            groupFileInfo.setGroupKey(groupKey);
            groupFileInfo.setGroupName(groupName);
            // 文件全放在一个分组内
            groupFileInfo.setFiles(newFileInfoList);
            newFileInfoList = new ArrayList<>();
            newFileInfoList.add(groupFileInfo);
        }
        System.out.println("fileGroupConfig:" + fileGroupConfig);
        System.out.println("fileConfigInfoList:" + fileConfigInfoList);
        return newFileInfoList;
    }

    /**
     * 生成配置文件
     *
     * @param newMeta
     * @param sourceRootPath
     * @param newFileInfoList
     * @param newModelInfoList
     * @return
     */
    private static Result generateConfig(Meta newMeta, String sourceRootPath, List<Meta.FileConfig.FileInfo> newFileInfoList, List<Meta.ModelConfig.ModelInfo> newModelInfoList, String templatePath) {
        String metaOutputPath = templatePath + File.separator + "meta.json";

        // 判断是否存在元文件，存在则在原文件基础上新增
        if (FileUtil.exist(metaOutputPath)) {
            Meta oldMeta = JSONUtil.toBean(FileUtil.readUtf8String(metaOutputPath), Meta.class);
            BeanUtil.copyProperties(newMeta, oldMeta, CopyOptions.create().ignoreNullValue());
            newMeta = oldMeta;

            // 追加文件配置参数
            List<Meta.FileConfig.FileInfo> fileInfoList = newMeta.getFileConfig().getFiles();
            fileInfoList.addAll(newFileInfoList);

            // 追加模型配置参数
            List<Meta.ModelConfig.ModelInfo> modelInfoList = newMeta.getModelConfig().getModels();
            modelInfoList.addAll(newModelInfoList);

            // 配置去重
            newMeta.getFileConfig().setFiles(distinctFiles(fileInfoList));
            newMeta.getModelConfig().setModels(distinctModels(modelInfoList));
        } else {
            // 构建配置参数对象
            Meta.FileConfig fileConfig = new Meta.FileConfig();
            newMeta.setFileConfig(fileConfig);
            fileConfig.setInputRootPath(sourceRootPath);
            List<Meta.FileConfig.FileInfo> fileInfoList = new ArrayList<>();
            fileConfig.setFiles(fileInfoList);
            fileInfoList.addAll(newFileInfoList);

            Meta.ModelConfig modelConfig = new Meta.ModelConfig();
            newMeta.setModelConfig(modelConfig);
            List<Meta.ModelConfig.ModelInfo> modelInfoList = new ArrayList<>();
            modelConfig.setModels(modelInfoList);
            modelInfoList.addAll(newModelInfoList);
        }
        Result result = new Result(newMeta, metaOutputPath);
        return result;
    }

    private static class Result {
        public final Meta newMeta;
        public final String metaOutputPath;

        public Result(Meta newMeta, String metaOutputPath) {
            this.newMeta = newMeta;
            this.metaOutputPath = metaOutputPath;
        }
    }

    /**
     * 文件去重
     *
     * @param fileInfoList 文件配置信息
     * @return
     */
    private static List<Meta.FileConfig.FileInfo> distinctFiles(List<Meta.FileConfig.FileInfo> fileInfoList) {

        // 1. 有分组与无分组划分
        Map<String, List<Meta.FileConfig.FileInfo>> groupKeyFileInfoListMap = fileInfoList.stream()
                .filter(fileInfo -> StrUtil.isNotBlank(fileInfo.getGroupKey()))
                .collect(
                        Collectors.groupingBy(Meta.FileConfig.FileInfo::getGroupKey)
                );

        // 2. 同组内配置进行合并, 对于有分组的文件配置，如果有相同分组，同分组内的文件进行合并 (merge), 不同分组的同时保留
        Map<String, Meta.FileConfig.FileInfo> groupKeyMergedFileInfoMap = new HashMap<>();
        for (Map.Entry<String, List<Meta.FileConfig.FileInfo>> entry : groupKeyFileInfoListMap.entrySet()) {
            List<Meta.FileConfig.FileInfo> tempFileInfoList = entry.getValue();
            List<Meta.FileConfig.FileInfo> newFileInfoList = new ArrayList<>(tempFileInfoList.stream()
                    // 展平文件列表
                    .flatMap(fileInfo -> fileInfo.getFiles().stream())
                    .collect(
                            // 进行去重, 如果有重复，则保留最后一个出现的文件对象
                            Collectors.toMap(Meta.FileConfig.FileInfo::getOutputPath, o -> o, (e, r) -> r)
                    ).values());
            // 使用新的 group 配置覆盖
            Meta.FileConfig.FileInfo newFileInfo = CollUtil.getLast(tempFileInfoList);
            newFileInfo.setFiles(newFileInfoList);
            String groupKey = entry.getKey();
            groupKeyMergedFileInfoMap.put(groupKey, newFileInfo);
        }

        // 3. 创建新的文件配置列表 (结果列表), 先将合并后的分组添加到 结果列表中
        ArrayList<Meta.FileConfig.FileInfo> resultList = new ArrayList<>(groupKeyMergedFileInfoMap.values());

        // 4. 将无分组的文件配置列表添加到 结果列表中
        resultList.addAll(new ArrayList<>(fileInfoList.stream()
                // 展平文件列表, 判断 GroupKey 是否存在
                .filter(fileInfo -> StrUtil.isBlank(fileInfo.getGroupKey()))
                .collect(
                        // 进行去重, 如果有重复，则保留最后一个出现的文件对象
                        Collectors.toMap(Meta.FileConfig.FileInfo::getOutputPath, o -> o, (e, r) -> r)
                ).values())
        );
        // 包含了去重并合并了同组内文件的配置信息，同时保留了无分组的文件配置
        return resultList;
    }

    /**
     * 模型去重
     */
    private static List<Meta.ModelConfig.ModelInfo> distinctModels(List<Meta.ModelConfig.ModelInfo> modelInfoList) {
        // 1. 有分组与无分组划分
        Map<String, List<Meta.ModelConfig.ModelInfo>> groupKeyModelInfoListMap = modelInfoList.stream()
                .filter(modelInfo -> StrUtil.isNotBlank(modelInfo.getGroupKey()))
                .collect(
                        Collectors.groupingBy(Meta.ModelConfig.ModelInfo::getGroupKey)
                );

        // 2. 同组内配置进行合并, 对于有分组的模型配置，如果有相同分组，同分组内的模型进行合并 (merge), 不同分组的同时保留
        Map<String, Meta.ModelConfig.ModelInfo> groupKeyMergedModelInfoMap = new HashMap<>();
        for (Map.Entry<String, List<Meta.ModelConfig.ModelInfo>> entry : groupKeyModelInfoListMap.entrySet()) {
            List<Meta.ModelConfig.ModelInfo> tempModelInfoList = entry.getValue();
            List<Meta.ModelConfig.ModelInfo> newModelInfoList = new ArrayList<>(tempModelInfoList.stream()
                    // 展平模型列表
                    .flatMap(modelInfo -> modelInfo.getModels().stream())
                    .collect(
                            // 进行去重, 如果有重复，则保留最后一个出现的模型对象
                            Collectors.toMap(Meta.ModelConfig.ModelInfo::getFieldName, o -> o, (e, r) -> r)
                    ).values());
            // 使用新的 group 配置覆盖
            Meta.ModelConfig.ModelInfo newModelInfo = CollUtil.getLast(tempModelInfoList);
            newModelInfo.setModels(newModelInfoList);
            String groupKey = entry.getKey();
            groupKeyMergedModelInfoMap.put(groupKey, newModelInfo);
        }

        // 3. 创建新的模型配置列表 (结果列表), 先将合并后的分组添加到 结果列表中
        ArrayList<Meta.ModelConfig.ModelInfo> resultList = new ArrayList<>(groupKeyMergedModelInfoMap.values());

        // 4. 将无分组的模型配置列表添加到 结果列表中
        resultList.addAll(new ArrayList<>(modelInfoList.stream()
                // 展平模型列表, 判断 GroupKey 是否存在
                .filter(modelInfo -> StrUtil.isBlank(modelInfo.getGroupKey()))
                .collect(
                        // 进行去重, 如果有重复，则保留最后一个出现的模型对象
                        Collectors.toMap(Meta.ModelConfig.ModelInfo::getFieldName, o -> o, (e, r) -> r)
                ).values())
        );
        // 包含了去重并合并了同组内模型的配置信息，同时保留了无分组的模型配置
        return resultList;
    }

    /**
     * 替换模板字段
     * 支持多个模型：对于同一个文件的内容，遍历模型进行多轮替换
     *
     * @param newFileContent
     * @param templateMakerModelConfig
     * @return
     */
    private static String replaceTemplateFields(String newFileContent, TemplateMakerModelConfig templateMakerModelConfig) {
        for (TemplateMakerModelConfig.ModelInfoConfig modelInfoConfig : templateMakerModelConfig.getModels()) {
            String replacement = templateMakerModelConfig.getModelGroupConfig() == null
                    ? String.format("${%s}", modelInfoConfig.getFieldName())
                    : String.format("${%s.%s}", templateMakerModelConfig.getModelGroupConfig().getGroupKey(), modelInfoConfig.getFieldName());
            newFileContent = StrUtil.replace(newFileContent, modelInfoConfig.getReplaceText(), replacement);
        }
        return newFileContent;
    }

    /**
     * 获取输入文件相对于源根路径的相对路径
     * @param inputFile
     * @param sourceRootPath
     * @return
     */
    private static String getRelativePath(File inputFile, String sourceRootPath) {
        String fileInputAbsolutePath = inputFile.getAbsolutePath().replace("\\", "/");
        return fileInputAbsolutePath.replace(sourceRootPath + "/", "");
    }

    /**
     * 获取文件内容，优先读取模板文件，否则读取源文件
     * @param fileOutputAbsolutePath
     * @param fileInputAbsolutePath
     * @return
     */
    private static String getFileContent(String fileOutputAbsolutePath, String fileInputAbsolutePath, boolean hasTemplateFile) {
        return FileUtil.exist(String.valueOf(hasTemplateFile)) ? FileUtil.readUtf8String(fileOutputAbsolutePath) : FileUtil.readUtf8String(fileInputAbsolutePath);
    }

    /**
     * 创建文件信息对象
     * @param fileInputPath
     * @param fileOutputPath
     * @return
     */
    private static Meta.FileConfig.FileInfo createFileInfo(String fileInputPath, String fileOutputPath) {
        Meta.FileConfig.FileInfo fileInfo = new Meta.FileConfig.FileInfo();
        fileInfo.setInputPath(fileOutputPath);
        fileInfo.setOutputPath(fileInputPath);
        fileInfo.setType(FileConfigValuesEnum.FILE.getValue());
        fileInfo.setGenerateType(FileConfigValuesEnum.DYNAMIC.getValue());
        return fileInfo;
    }
}
