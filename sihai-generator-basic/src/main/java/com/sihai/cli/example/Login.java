package com.sihai.cli.example;

import picocli.CommandLine;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

public class Login implements Callable<Integer> {

    @Option(names = {"-u", "--user"}, description = "User name")
    String user;

    // 开启交互式输入密码
    @Option(names = {"-p", "--password"}, description = "passphrase", arity = "0..1", interactive = true, prompt = "请输入密码：")
    String password;

    @Option(names = {"-cp", "--checkPassword"}, description = "Check Passphrase", arity = "0..1", interactive = true, prompt = "请再次输入密码：")
    String checkPassword;

    @Override
    public Integer call() throws Exception {
        System.out.println("user = " + user);
        System.out.println("password = " + password);
        System.out.println("checkPassword = " + checkPassword);
        return 0;
    }

    public static void main(String[] args) {
        new CommandLine(new Login()).execute("-u", "user123", "-p");
    }
}
