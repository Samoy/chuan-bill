package com.samoy.chuanbillserver.utils;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.VelocityTemplateEngine;

import java.util.Collections;

public class MybatisCodeGeneration {
    public static void main(String[] args) {
        System.out.println("用户目录：" + System.getProperty("user.dir"));
        FastAutoGenerator.create("jdbc:mysql://localhost:3306/chuan_bill?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai",
                        "root", "123456")
                .globalConfig(builder -> {
                    builder.author("Samoy")

                            .outputDir(System.getProperty("user.dir") + "/chuan-bill-server/src/main/java");

                }).packageConfig(builder -> {
                    builder.parent("com.samoy.chuanbillserver")
                            .entity("entity")
                            .mapper("dao")
                            .service("service")
                            .serviceImpl("service.impl")
                            .xml("mapper.xml")
                            .pathInfo(Collections.singletonMap(OutputFile.xml, System.getProperty("user.dir") + "/chuan-bill-server/src/main/resources/mapper"));
                })
                .strategyConfig(builder -> {
                    builder.addTablePrefix("t_")
                            .entityBuilder()
                            .enableLombok()
                            .enableFileOverride()
                            .enableTableFieldAnnotation()
                            .controllerBuilder()
                            .disable();
                })
                .templateEngine(new VelocityTemplateEngine())
                .execute();
    }
}
