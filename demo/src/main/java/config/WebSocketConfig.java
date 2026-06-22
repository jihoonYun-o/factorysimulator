package config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // WebSocket 메시지 브로커 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 1. 클라이언트(프론트엔드)가 데이터를 받을 때(구독) 사용할 경로
        // 예: /topic/oht-locations 를 구독하면 실시간 위치를 받음
        config.enableSimpleBroker("/topic");
        
        // 2. 클라이언트가 서버로 데이터를 보낼 때 사용할 경로의 접두사
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 3. 프론트엔드에서 최초로 WebSocket 연결을 맺을 엔드포인트 주소
        registry.addEndpoint("/ws-oht")
                .setAllowedOriginPatterns("*") // CORS 문제 방지 (모든 도메인 허용)
                .withSockJS(); // WebSocket을 지원하지 않는 환경에서도 통신 가능하게 해주는 옵션
    }
}
