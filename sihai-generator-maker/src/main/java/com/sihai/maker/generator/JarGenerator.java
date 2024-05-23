package com.sihai.maker.generator;

import java.io.*;
import java.util.Map;

/**
 * jar 包生成器
 */
public class JarGenerator {

    /**
     * 生成 jar 包
     * @param projectDir
     * @throws IOException
     * @throws InterruptedException
     */
    public static void doGenerate(String projectDir) throws IOException, InterruptedException {

        // 清理之前的构建并打包
        // 注意不同操作系统，执行的命令不同
        String winMavenCommand = "mvn.cmd clean package -DskipTests=true";
        String otherMavenCommand = "mvn clean package -DskipTests=true";
        String mavenCommand = winMavenCommand;

        // 这里一定要拆分！
        ProcessBuilder processBuilder = new ProcessBuilder(mavenCommand.split(" "));
        // 对 projectDir 进行基本验证，防止命令注入（实际中需要更复杂的验证）
        if (!projectDir.contains("..")) {
            try {
                // 指定执行文件路径
                processBuilder.directory(new File(projectDir));
                Process process = processBuilder.start();

                // 使用try-with-resources确保资源被释放
                try (InputStream inputStream = process.getInputStream();
                     BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {

                    // 输出打印处理
                    printProcessOutput(bufferedReader);

                    // 等待命令执行结束并获取状态码
                    int exitCode = process.waitFor();
                    // 增加日志记录
                    System.out.println("命令执行结束，结束码：" + exitCode);
                }
            } catch (IOException e) {
                // 异常处理
                System.err.println("IO异常: " + e.getMessage());
            } catch (InterruptedException e) {
                // 异常处理
                System.err.println("线程中断异常: " + e.getMessage());
                // 重新设置中断状态
                Thread.currentThread().interrupt();
            }
        } else {
            System.err.println("项目路径包含非法字符，无法执行命令");
        }
    }

    /**
     * 输出打印处理
     * @param bufferedReader
     */
    private static void printProcessOutput(BufferedReader bufferedReader) {
        String line;
        try {
            while ((line = bufferedReader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.err.println("读取命令行输出时发生IO异常: " + e.getMessage());
        }
    }
}
