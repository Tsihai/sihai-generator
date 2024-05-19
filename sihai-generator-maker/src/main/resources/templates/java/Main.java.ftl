package ${basePackage};

import ${basePackage}.cli.CommandExecutor;

public class Main {

    public static void main(String[] args) {
        // 创建命令执行器
        CommandExecutor commandExecutor = new CommandExecutor();
        // 执行命令
        commandExecutor.doExecute(args);
    }
}