package com.sihai.generator;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** 静态文件生成器
 * @Author sihai
 * @Date 2024/1/8 11:24
 */
public class StaticGenerator {

    public static void main(String[] args) {
        String projectPath = System.getProperty("user.dir");
        // 输入路径
        String inputPath = projectPath + File.separator + "sihai-generator-demo" + File.separator + "acm-template";
        // 目标路径
        String outputPath = projectPath;
        // 调用拷贝方法
        copyFilesByHutool(inputPath, outputPath);
    }

    /**
     * 拷贝文件
     * 使用 hutool 工具类 copy 方法
     * @param inputPath 输入路径
     * @param outputPath 目标路径
     */
    public static void copyFilesByHutool(String inputPath, String outputPath) {
        // 源文件路径、目标文件路径、是否覆盖
        FileUtil.copy(inputPath, outputPath, false);
    }


    /**
     * 以下是递归遍历文件的方法
     */
    public static void copyFilesByRecursive(String inputPath, String outputPath) {
        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);

        try {
            copyFileByRecursive(inputFile, outputFile);
        } catch (Exception e) {
            System.out.println("文件复制失败");
            e.printStackTrace();
        }
    }

    /**
     * 先创建目录，再遍历目录内的文件，依次复制
     */
    public static void copyFileByRecursive(File inputFile, File outputFile) throws IOException {

        // 首先区分是目录还是文件
        if (inputFile.isDirectory()) {
            // 输入文件名称
            System.out.println(inputFile.getName());
            File destOutputFile = new File(outputFile, inputFile.getName());
            // 如果是目录，首先创建目录
            if (!destOutputFile.exists()) {
                destOutputFile.mkdirs();
            }
            // 获取目录下的所有文件和子目录
            File[] files = inputFile.listFiles();
            // 如果没有子目录，直接结束
            if (ArrayUtil.isEmpty(files)) {
                return;
            }
            // 遍历子目录
            for (File file : files) {
                // 递归复制下一层文件
                copyFileByRecursive(file, destOutputFile);
            }
        } else {
            // 如果是文件，直接复制到目标文件夹
            Path destPath = outputFile.toPath().resolve(inputFile.getName());
            // 指定覆盖已存在的文件
            Files.copy(inputFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }


}
