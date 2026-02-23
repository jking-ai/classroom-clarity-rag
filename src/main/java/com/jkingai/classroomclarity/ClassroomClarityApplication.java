package com.jkingai.classroomclarity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClassroomClarityApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClassroomClarityApplication.class, args);
    }
}
