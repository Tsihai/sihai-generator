package com.sihai.maker.generator.main;

/**
 * 生成代码生成器压缩包
 */
public class ZipGenerator extends GenerateTemplate {

    @Override
    protected String createDistPackage(String outputPath, String sourceCopyDestPath, String jarPath, String shellOutputFilePath, String sourceRootPath) {
        String distPath = super.createDistPackage(outputPath, sourceCopyDestPath, jarPath, shellOutputFilePath, sourceRootPath);
        return super.buildZip(distPath);
    }
}