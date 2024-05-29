package com.sihai.maker.generator.main;

/**
 * 核心生成器 (可拓展通用类)
 */
public class MainGenerator extends GenerateTemplate{

    @Override
    protected String createDistPackage(String outputPath, String sourceCopyDestPath, String jarPath, String shellOutputFilePath, String sourceRootPath) {
        System.out.println("不要给我输出 dist 啦！");
        return "";
    }
}
