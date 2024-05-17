package com.sihai.cli;

import com.sihai.cli.command.ConfigCommand;
import com.sihai.cli.command.GenerateCommand;
import com.sihai.cli.command.ListCommand;
import picocli.CommandLine;
import picocli.CommandLine.Command;

/**
 * 命令执行器
 */
@Command(name = "sihai", mixinStandardHelpOptions = true)
public class CommandExecutor implements Runnable {

    private final CommandLine commandLine;

    {
        commandLine = new CommandLine(this)
                .addSubcommand(new GenerateCommand())
                .addSubcommand(new ConfigCommand())
                .addSubcommand(new ListCommand());
    }

    @Override
    public void run() {
        System.out.println("请输入具体命令, 或者使用 --help 查看帮助");
    }

    /**
     * 执行命令
     */
    public Integer doExecute(String[] args) {
        return commandLine.execute(args);
    }
}
