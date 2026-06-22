package config;


import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedissionConfig {

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        // 우리가 도커로 띄운 Redis 주소와 비밀번호 입력
        config.useSingleServer()
              .setAddress("redis://localhost:6379")
              .setPassword("1234");
        return Redisson.create(config);
    }
}
