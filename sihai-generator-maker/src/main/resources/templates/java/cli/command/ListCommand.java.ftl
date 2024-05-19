package ${basePackage}.cli.command;

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
        // 输入路径
        String inputPath = "${fileConfig.inputRootPath}";
        // 遍历获取文件列表
        List<File> files = FileUtil.loopFiles(inputPath);
        for (File file : files) {
            System.out.println(file);
        }
    }
}
