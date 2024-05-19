package ${basePackage}.generator;

import cn.hutool.core.io.FileUtil;

/** 静态文件生成器
 * @Author sihai
 * @Date 2024/1/8 11:24
 */
public class StaticGenerator {

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

}
