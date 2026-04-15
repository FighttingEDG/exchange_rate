package shop.shanjunwei.ratefetcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan  // 扫描 @ConfigurationProperties 注解的类
@EnableScheduling
public class RateFetcherApplication {

    public static void main(String[] args) {
        SpringApplication.run(RateFetcherApplication.class, args);
    }
}
