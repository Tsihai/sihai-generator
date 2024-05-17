package com.sihai.cli.command;

import cn.hutool.core.util.ReflectUtil;
import com.sihai.model.MainTemplateConfig;
import picocli.CommandLine;

import java.lang.reflect.Field;
import java.util.Arrays;

/**
 * 子命令：查看允许客户传入的动态参数
 */
@CommandLine.Command(name = "config", mixinStandardHelpOptions = true)
public class ConfigCommand implements Runnable{


    @Override
    public void run() {
        System.out.println("查看参数信息");

        // 使用hutool工具类反射获取当前类的所有字段
        Field[] fields = ReflectUtil.getFields(MainTemplateConfig.class);

        // 遍历打印每个字段信息
        for (Field field : fields) {
            System.out.println("字段名称：" + field.getName());
            System.out.println("字段类型：" + field.getType());
            System.out.println("-------");
        }
    }
}
