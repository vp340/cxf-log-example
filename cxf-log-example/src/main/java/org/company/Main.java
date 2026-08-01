package org.company;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
import org.springframework.stereotype.Component;

@SpringBootApplication
@ImportResource("classpath:/spring/camel-context.xml")
public class Main {

    public static void main(String[] args) {
        SpringApplication.run(Main.class, (args));
    }

    @Component
    public static class StartupListener implements ApplicationRunner {

        @Override
        public void run(ApplicationArguments args) {
        }
    }
}