package com.sihai.cli.command;

import cn.hutool.core.io.FileUtil;
import picocli.CommandLine;

import java.io.File;
import java.util.List;

/**
 * 子命令：遍历所有准备生成的文件列表
 */
@CommandLine.Command(name = "list", mixinStandardHelpOptions = true, description = "查看文件列表")
public class ListCommand implements Runnable{

    public void run() {
        // 获取当前项目路径
        String projectPath = System.getProperty("user.dir");
        // 获取整个项目的根路径
        File parentFile = new File(projectPath).getParentFile();
        // 获取输入路径
        String inputPath = new File(parentFile, "sihai-generator-demo/acm-template").getAbsolutePath();
        // 遍历获取文件列表
        List<File> files = FileUtil.loopFiles(inputPath);
        for (File file : files) {
            System.out.println(file);
        }
    }
}
