package controller;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.fasterxml.jackson.databind.ObjectMapper;

@EnableScheduling // 스케줄러 활성화 추가!
@SpringBootApplication(scanBasePackages = {"demo", "controller", "service", "config", "dto"}) class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
