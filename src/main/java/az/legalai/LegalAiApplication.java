package az.legalai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LegalAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(LegalAiApplication.class, args);
    }
}
