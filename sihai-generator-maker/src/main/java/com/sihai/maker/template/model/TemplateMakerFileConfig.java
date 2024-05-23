package com.sihai.maker.template.model;

import com.sihai.maker.meta.Meta;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模板生成器文件配置
 */
@Data
public class TemplateMakerFileConfig {

    /**
     * 文件配置
     */
    private List<FileInfoConfig> files;

    /**
     * 文件组配置
     */
    private FileGroupConfig fileGroupConfig;

    @Data
    @NoArgsConstructor
    public static class FileInfoConfig {

        private String path;

        private List<FileFilterConfig> filterConfigList;
    }

    @Data
    public static class FileGroupConfig {

        private String condition;

        private String groupKey;

        private String groupName;
    }
}
