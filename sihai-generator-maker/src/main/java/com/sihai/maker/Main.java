package com.sihai.maker;

import com.sihai.maker.generator.main.GenerateTemplate;
import com.sihai.maker.generator.main.MainGenerator;
import com.sihai.maker.generator.main.ZipGenerator;
import freemarker.template.TemplateException;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws TemplateException, IOException, InterruptedException {
        GenerateTemplate generateTemplate = new ZipGenerator();
        generateTemplate.doGenerate();
    }
}