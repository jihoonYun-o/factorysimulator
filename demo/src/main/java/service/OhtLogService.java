package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dto.OhtData;
import controller.entity.OhtLog;
import controller.repository.OhtLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j // 로그 출력을 위한 어노테이션
@Service
@RequiredArgsConstructor
public class OhtLogService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final OhtLogRepository ohtLogRepository;

    // ⭐️ 10초(10000ms)마다 백그라운드에서 조용히 실행됨
    @Scheduled(fixedRate = 10000)
    @Transactional
    public void saveLogBatch() {
        try {
            // 1. Redis에서 현재 모든 OHT의 최신 상태(스냅샷)를 가져옴
            Map<Object, Object> entries = redisTemplate.opsForHash().entries("OHT_CACHE");
            List<OhtLog> logsToSave = new ArrayList<>();

            for (Object value : entries.values()) {
                OhtData data = objectMapper.readValue(value.toString(), OhtData.class);
                // 2. DB에 넣을 Entity 객체로 변환하여 리스트에 담음
                logsToSave.add(new OhtLog(data.getOhtId(), data.getX(), data.getY(), data.getStatus()));
            }

            // 3. DB에 한 번에 밀어 넣기 (Batch Insert)
            if (!logsToSave.isEmpty()) {
                ohtLogRepository.saveAll(logsToSave);
                log.info("✅ [Data Pipeline] {}대의 OHT 상태 로그를 DB에 비동기 저장했습니다.", logsToSave.size());
            }
            
        } catch (Exception e) {
            log.error("로그 저장 중 에러 발생", e);
        }
    }
}
