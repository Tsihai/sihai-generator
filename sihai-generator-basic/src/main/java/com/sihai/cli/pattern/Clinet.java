package com.sihai.cli.pattern;

/**
 * 客户端
 */
public class Clinet {

    public static void main(String[] args) {
        // 创建接受者对象
        Device tv = new Device("电视机");
        Device light = new Device("电灯");

        // 创建具体命令对象，并设定它的接受者
        Command turnOn = new TurnOnCommand(tv);
        Command turnOff = new TurnOffCommand(light);

        // 创建调用者，将命令对象设置进去
        RemoteControl remoteControl = new RemoteControl();

        // 设置命令并执行
        remoteControl.setCommand(turnOn);
        remoteControl.pressButton();

        remoteControl.setCommand(turnOff);
        remoteControl.pressButton();
    }
}
