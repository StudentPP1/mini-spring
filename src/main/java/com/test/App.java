package com.test;

import com.test.annotation.SpringBootApplication;
import com.test.initializer.SpringApplication;

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class);
    }
}