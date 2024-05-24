package com.sihai.maker.template;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.sihai.maker.meta.Meta;
import com.sihai.maker.meta.enums.FileConfigValuesEnum;
import com.sihai.maker.template.Filter.FileFilter;
import com.sihai.maker.template.enums.FileFilterRangeEnum;
import com.sihai.maker.template.enums.FileFilterRuleEnum;
import com.sihai.maker.template.model.FileFilterConfig;
import com.sihai.maker.template.model.TemplateMakerFileConfig;
import com.sihai.maker.template.model.TemplateMakerModelConfig;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 模版制作工具
 */
public class TemplateMakerPro {

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
    private static long makeTemplate(Meta newMeta, String originProjectPath, TemplateMakerFileConfig templateMakerFileConfig, TemplateMakerModelConfig templateMakerModelConfig, Long id) {
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
        Result result = generateConfig(newMeta, sourceRootPath, newFileInfoList, newModelInfoList);

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

        // 读取文件内容，判断：如果已有模版文件，则在原有模版文件中挖坑
        String fileContent = getFileContent(fileOutputAbsolutePath, fileInputAbsolutePath);

        // 创建文件信息对象
        String newFileContent = replaceTemplateFields(fileContent, templateMakerModelConfig);

        // 创建文件信息对象
        Meta.FileConfig.FileInfo fileInfo = createFileInfo(fileInputPath, newFileContent);

        // 判断是否和源文件相同，则不生成模版文件，静态生成
        if (newFileContent.equals(fileContent)) {
            fileInfo.setOutputPath(fileInputPath);
            fileInfo.setGenerateType(FileConfigValuesEnum.STATIC.getValue());
        } else {
            fileInfo.setGenerateType(FileConfigValuesEnum.DYNAMIC.getValue());
            // 输出模版文件
            FileUtil.writeUtf8String(newFileContent, fileOutputAbsolutePath);
        }
        return fileInfo;
    }

    public static void main(String[] args) {
        Meta meta = new Meta();
        meta.setName("acm-template-generator");
        meta.setDescription("ACM 示例模版生成器");

        // 指定原始项目路径
        String projectPath = System.getProperty("user.dir");
        String originProjectPath = FileUtil.getAbsolutePath(new File(projectPath).getParentFile()) + File.separator + "sihai-generator-demo/springboot-init";

        String fileInputPath = "src/main/java/com/sihai/springbootinit/common";
        String fileInputPath2 = "src/main/resources/application.yml";
        List<String> fileInputPathList = Arrays.asList(fileInputPath, fileInputPath2);

        // 输入模型参数信息
        Meta.ModelConfig.ModelInfo modelInfo = new Meta.ModelConfig.ModelInfo();
//        modelInfo.setFieldName("outputText");
//        modelInfo.setType("String");
//        modelInfo.setDefaultValue("sum = ");

        modelInfo.setFieldName("className");
        modelInfo.setType("String");

//        String searchStr = "Sum = ";
        String searchStr = "BaseResponse";

        // 文件过滤配置
        TemplateMakerFileConfig.FileInfoConfig fileInfoConfig = new TemplateMakerFileConfig.FileInfoConfig();
        fileInfoConfig.setPath(fileInputPath);
        List<FileFilterConfig> fileFilterConfigList = new ArrayList<>();
        FileFilterConfig fileFilterConfig = FileFilterConfig.builder()
                .range(FileFilterRangeEnum.FILE_NAME.getValue())
                .rule(FileFilterRuleEnum.CONTAINS.getValue())
                .value("Base")
                .build();
        fileFilterConfigList.add(fileFilterConfig);
        fileInfoConfig.setFilterConfigList(fileFilterConfigList);


        TemplateMakerFileConfig.FileInfoConfig fileInfoConfig2 = new TemplateMakerFileConfig.FileInfoConfig();
        fileInfoConfig2.setPath(fileInputPath2);

        List<TemplateMakerFileConfig.FileInfoConfig> fileInfoConfigList = Arrays.asList(fileInfoConfig, fileInfoConfig2);

        TemplateMakerFileConfig templateMakerFileConfig = new TemplateMakerFileConfig();
        templateMakerFileConfig.setFiles(fileInfoConfigList);

        // 文件分组配置
        TemplateMakerFileConfig.FileGroupConfig fileGroupConfig = new TemplateMakerFileConfig.FileGroupConfig();
        fileGroupConfig.setCondition("outputText");
        fileGroupConfig.setGroupKey("test");
        fileGroupConfig.setGroupName("测试分组");
        templateMakerFileConfig.setFileGroupConfig(fileGroupConfig);

        // 模型参数配置
        TemplateMakerModelConfig templateMakerModelConfig = new TemplateMakerModelConfig();

        // 模型组配置
        TemplateMakerModelConfig.ModelGroupConfig modelGroupConfig = new TemplateMakerModelConfig.ModelGroupConfig();
        modelGroupConfig.setGroupKey("mysql");
        modelGroupConfig.setGroupName("数据库配置");
        templateMakerModelConfig.setModelGroupConfig(modelGroupConfig);

        // 模型配置1
        TemplateMakerModelConfig.ModelInfoConfig modelInfoConfig = new TemplateMakerModelConfig.ModelInfoConfig();
        modelInfoConfig.setFieldName("url");
        modelInfoConfig.setType("String");
        modelInfoConfig.setDefaultValue("jdbc:mysql://localhost:3306/my_db");
        modelInfoConfig.setReplaceText("jdbc:mysql://localhost:3306/sihai_db");

        // 模型配置2
        TemplateMakerModelConfig.ModelInfoConfig modelInfoConfig2 = new TemplateMakerModelConfig.ModelInfoConfig();
        modelInfoConfig2.setFieldName("username");
        modelInfoConfig2.setType("String");
        modelInfoConfig2.setDefaultValue("root");
        modelInfoConfig2.setReplaceText("root");

        List<TemplateMakerModelConfig.ModelInfoConfig> modelInfoConfigList = Arrays.asList(modelInfoConfig, modelInfoConfig2);
        templateMakerModelConfig.setModels(modelInfoConfigList);

        long id = TemplateMakerPro.makeTemplate(meta, originProjectPath, templateMakerFileConfig, templateMakerModelConfig, 1793634202667778559L);
        System.out.println(id);
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
        String sourceRootPath = templatePath + File.separator + FileUtil.getLastPathEle(Paths.get(originProjectPath)).toString();
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
        List<TemplateMakerModelConfig.ModelInfoConfig> models = templateMakerModelConfig.getModels();
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
     */
    private static List<Meta.ModelConfig.ModelInfo> processModelGroup(TemplateMakerModelConfig templateMakerModelConfig, List<Meta.ModelConfig.ModelInfo> inputModelInfoList) {
        List<Meta.ModelConfig.ModelInfo> newModelInfoList = new ArrayList<>();
        TemplateMakerModelConfig.ModelGroupConfig modelGroupConfig = templateMakerModelConfig.getModelGroupConfig();

        if (modelGroupConfig != null) {
            Meta.ModelConfig.ModelInfo groupModelInfo = new Meta.ModelConfig.ModelInfo();
            // 避免直接访问可能为null的对象属性，使用Optional包装
            String condition = Optional.ofNullable(modelGroupConfig.getCondition()).orElse("");
            String groupKey = Optional.ofNullable(modelGroupConfig.getGroupKey()).orElse("");
            String groupName = Optional.ofNullable(modelGroupConfig.getGroupName()).orElse("");

            groupModelInfo.setCondition(condition);
            groupModelInfo.setGroupKey(groupKey);
            groupModelInfo.setGroupName(groupName);
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
        List<TemplateMakerFileConfig.FileInfoConfig> fileInfoConfigList = templateMakerFileConfig.getFiles();
        // 遍历输入文件
        List<Meta.FileConfig.FileInfo> newFileInfoList = new ArrayList<>();
        for (TemplateMakerFileConfig.FileInfoConfig fileInfoConfig : fileInfoConfigList) {
            String inputFilePath = fileInfoConfig.getPath();
            String inputFileAbsolutePath = sourceRootPath + File.separator + inputFilePath;

            // 传入绝对路径, 得到过滤后的文件列表
            List<File> fileList = FileFilter.doFilter(inputFileAbsolutePath, fileInfoConfig.getFilterConfigList());
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
    private static Result generateConfig(Meta newMeta, String sourceRootPath, List<Meta.FileConfig.FileInfo> newFileInfoList, List<Meta.ModelConfig.ModelInfo> newModelInfoList) {
        String metaOutputPath = sourceRootPath + File.separator + "meta.json";

        // 判断是否存在元文件，存在则在原文件基础上新增
        if (FileUtil.exist(metaOutputPath)) {
            newMeta = JSONUtil.toBean(FileUtil.readUtf8String(metaOutputPath), Meta.class);
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
                            Collectors.toMap(Meta.FileConfig.FileInfo::getInputPath, o -> o, (e, r) -> r)
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
                        Collectors.toMap(Meta.FileConfig.FileInfo::getInputPath, o -> o, (e, r) -> r)
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
     * @param content
     * @param templateMakerModelConfig
     * @return
     */
    private static String replaceTemplateFields(String content, TemplateMakerModelConfig templateMakerModelConfig) {
        for (TemplateMakerModelConfig.ModelInfoConfig modelInfoConfig : templateMakerModelConfig.getModels()) {
            String replacement = templateMakerModelConfig.getModelGroupConfig() == null
                    ? String.format("${%s}", modelInfoConfig.getFieldName())
                    : String.format("${%s.%s}", templateMakerModelConfig.getModelGroupConfig().getGroupKey(), modelInfoConfig.getFieldName());
            content = StrUtil.replace(content, modelInfoConfig.getReplaceText(), replacement);
        }
        return content;
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
    private static String getFileContent(String fileOutputAbsolutePath, String fileInputAbsolutePath) {
        return FileUtil.exist(fileOutputAbsolutePath) ? FileUtil.readUtf8String(fileOutputAbsolutePath) : FileUtil.readUtf8String(fileInputAbsolutePath);
    }

    /**
     * 创建文件信息对象
     * @param fileInputPath
     * @param newFileContent
     * @return
     */
    private static Meta.FileConfig.FileInfo createFileInfo(String fileInputPath, String newFileContent) {
        Meta.FileConfig.FileInfo fileInfo = new Meta.FileConfig.FileInfo();
        fileInfo.setInputPath(fileInputPath);
        fileInfo.setType(FileConfigValuesEnum.FILE.getValue());
        return fileInfo;
    }
}
