package com.meituan.demo.backend.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.meituan.demo.backend.persistence.mapper")
public class MyBatisConfig {
}
