package service;



import dto.OhtData;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import lombok.extern.slf4j.Slf4j;


import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisSubscriber {

    // WebSocket으로 메시지를 쏴주는 템플릿
    private final SimpMessageSendingOperations messagingTemplate;
    // JSON 문자열을 자바 객체로 변환해주는 매퍼
    private final ObjectMapper objectMapper;

    // Redis에서 메시지가 발행(Publish)되면 이 메서드가 실행됩니다.
    public void sendMessage(String publishMessage) {
        try {
            // 1. Redis에서 넘어온 JSON 문자열을 OhtData 객체로 변환
            dto.OhtData ohtData = objectMapper.readValue(publishMessage, dto.OhtData.class);
            
            // 2. WebSocket을 구독 중인 프론트엔드 클라이언트들에게 데이터 전송
            messagingTemplate.convertAndSend("/topic/oht", ohtData);
            
        } catch (Exception e) {
            log.error("Redis Subscriber Error", e);
        }
    }
}

