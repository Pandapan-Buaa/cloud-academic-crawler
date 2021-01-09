package com.sheep.cloud.academic.crawler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sheep.cloud.academic.crawler.controller.ScholarTempController;

/**
 * @author YangChao
 */
@ComponentScan(basePackages = "com.sheep.cloud")
@RestController
@SpringBootApplication
public class ServletInitializer extends SpringBootServletInitializer {

    @RequestMapping("/")
    public String hello() {
        return "Hello, cloud-academic-crawler!";
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ServletInitializer.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(ServletInitializer.class, args);
        if (args.length != 0) {
            System.out.println(new ScholarTempController().parseRequest(args));
        }
    }
}