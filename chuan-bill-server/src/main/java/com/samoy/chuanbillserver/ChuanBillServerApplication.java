package com.samoy.chuanbillserver;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.samoy.chuanbillserver.dao")
public class ChuanBillServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChuanBillServerApplication.class, args);
    }

}
