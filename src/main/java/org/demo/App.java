package org.demo;

import org.spring.annotation.SpringBootApplication;
import org.spring.initializer.SpringApplication;

import java.util.TimeZone;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("Europe/Kyiv"));
        SpringApplication.run(App.class);
    }
}