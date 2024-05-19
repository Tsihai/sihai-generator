package com.sihai.maker.generator;

import java.io.*;

public class JarGenerator {

    public static void doGenerate(String projectDir) throws IOException, InterruptedException {
        // 调用 Process 类执行打包命令
        String winMavenCommand = "mvn.cmd clean package -DskipTests=true";
        String otherMavenCommand = "mvn clean package -DskipTests=true";
        String mavenCommand = winMavenCommand;

        ProcessBuilder processBuilder = new ProcessBuilder(mavenCommand.split(" "));
        // 指定执行文件路径
        processBuilder.directory(new File(projectDir));
        Process process = processBuilder.start();

        // 读取命令行输出
        InputStream inputStream = process.getInputStream();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            System.out.println(line);
        }

        // 等待命令执行结束并获取状态码
        int exitCode = process.waitFor();
        System.out.println("命令执行结束，结束码：" + exitCode);
    }
}
