package com.sihai.cli.command;

import cn.hutool.core.bean.BeanUtil;
import com.sihai.generator.DynamicGenerator;
import com.sihai.generator.MainGenerator;
import com.sihai.model.MainTemplateConfig;
import lombok.Data;
import picocli.CommandLine.Option;
import picocli.CommandLine.Command;

import java.util.concurrent.Callable;

/**
 * 子命令：生成代码
 * 作用：接受参数生成代码
 */
@Data
@Command(name = "generate", mixinStandardHelpOptions = true, description = "生成代码")
public class GenerateCommand implements Callable<Integer> {

    @Option(names = {"-l", "--loop"}, arity = "0..1", description = "是否循环生成代码", interactive = true, echo = true)
    private boolean loop;

    @Option(names = {"-a", "--author"}, arity = "0..1", description = "作者", interactive = true, echo = true)
    private String author = "sihai";

    @Option(names = {"-o", "--outputText"}, arity = "0..1", description = "输出文本", interactive = true, echo = true)
    private String outputText = "sum = ";

    public Integer call() throws Exception {
        MainTemplateConfig mainTemplateConfig = new MainTemplateConfig();
        BeanUtil.copyProperties(this, mainTemplateConfig);
        System.out.println("配置信息：" + mainTemplateConfig);
        MainGenerator.doGenerate(mainTemplateConfig);
        return 0;
    }

}
