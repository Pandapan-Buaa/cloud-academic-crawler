package com.sheep.cloud.academic.crawler.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author YangChao
 * @create 2019-03-19 12:03
 **/
@Configuration
public class JdbcConfig {

    @Bean(name = "jdbcTemplate")
    public JdbcTemplate jdbcTemplate() {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
       // dataSource.setJdbcUrl("jdbc:mysql://192.168.0.84:3306/academic?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai");
        dataSource.setJdbcUrl("jdbc:mysql://192.168.0.84:3306/dump?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai");
        // dataSource.setJdbcUrl("jdbc:mysql://192.168.0.90:3306/dump?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai");
        // dataSource.setJdbcUrl("jdbc:mysql://192.168.0.90:3306/academic?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai");
        dataSource.setUsername("root");
        dataSource.setPassword("root");
        return new JdbcTemplate(dataSource);
    }

}