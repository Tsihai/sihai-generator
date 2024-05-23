package com.sihai.maker.template.Filter;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import com.sihai.maker.template.enums.FileFilterRangeEnum;
import com.sihai.maker.template.enums.FileFilterRuleEnum;
import com.sihai.maker.template.model.FileFilterConfig;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件过滤器
 */
public class FileFilter {

    /**
     * 单个文件过滤
     */
    public static boolean doSingleFileFilter(List<FileFilterConfig> fileFilterConfigList, File file) {
        // 文件名称
        String fileName = file.getName();
        // 文件内容
        String fileContent = FileUtil.readUtf8String(file);

        // 所有过滤器校验结束后的结果
        boolean result = true;

        if (fileFilterConfigList == null || fileFilterConfigList.isEmpty()) {
            return true;
        }

        for (FileFilterConfig fileFilterConfig : fileFilterConfigList) {
            String range = fileFilterConfig.getRange();
            String rule = fileFilterConfig.getRule();
            String value = fileFilterConfig.getValue();

            // 文件范围枚举值校验
            FileFilterRangeEnum fileFilterRangeEnum = FileFilterRangeEnum.getEnumByValue(range);
            if (fileFilterRangeEnum == null) {
                continue;
            }

            String content = fileName;
            switch (fileFilterRangeEnum) {
                case FILE_NAME:
                    // 文件名称过滤
                    content = fileName;
                    break;
                case FILE_CONTENT:
                    // 文件内容过滤
                    content = fileContent;
                    break;
                default:
                    break;
            }

            // 文件规则枚举值校验
            FileFilterRuleEnum fileFilterRuleEnum = FileFilterRuleEnum.getEnumByValue(rule);
            if (fileFilterRuleEnum == null) {
                continue;
            }

            switch (fileFilterRuleEnum) {
                case EQUAL:
                    // 等于
                    result = content.equals(value);
                    break;
                case CONTAINS:
                    // 包含
                    result = content.contains(value);
                    break;
                case STARTS_WITH:
                    // 前缀匹配
                    result = content.startsWith(value);
                    break;
                case ENDS_WITH:
                    // 后缀匹配
                    result = content.endsWith(value);
                    break;
                case REGEX:
                    // 正则表达式
                    result = content.matches(value);
                    break;
                default:
                    break;
            }

            // 如果存在一个过滤器校验失败，则返回false
            if (!result) {
                return false;
            }
        }

        return true;
    }

    /**
     * 对某个文件或目录进行过滤，返回文件列表
     */
    public static List<File> doFilter(String filePath, List<FileFilterConfig> fileFilterConfigList) {
        // 根据路径获取所有文件
        List<File> fileList = FileUtil.loopFiles(filePath);
        return fileList.stream()
                .filter(file -> doSingleFileFilter(fileFilterConfigList, file))
                .collect(Collectors.toList());
    }
}
