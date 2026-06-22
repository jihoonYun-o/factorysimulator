package controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import dto.OhtData;
import controller.entity.OhtLog;
import controller.repository.OhtLogRepository;
import service.OhtSimulator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/oht")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OhtController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final OhtSimulator ohtSimulator;
    
    // ⭐️ DB 저장소 연결 추가!
    private final OhtLogRepository ohtLogRepository; 

    @GetMapping("/locations")
    public List<OhtData> getAllOhtLocations() {
        List<OhtData> ohtList = new ArrayList<>();
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("OHT_CACHE");
        for (Object value : entries.values()) {
            try {
                ohtList.add(objectMapper.readValue(value.toString(), OhtData.class));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return ohtList;
    }

    @PostMapping("/command")
    public void sendCommand(@RequestBody Map<String, String> payload) {
        String ohtId = payload.get("ohtId");
        String command = payload.get("command");
        ohtSimulator.executeCommand(ohtId, command);
    }

    // ⭐️ [핵심] DB에 저장된 로그를 직접 꺼내서 눈으로 확인하는 API
    @GetMapping("/logs")
    public List<OhtLog> getDbLogs() {
        // DB(H2)에 10초마다 쌓인 모든 로그를 가져와서 화면에 뿌려줍니다.
        return ohtLogRepository.findAll();
    }
    
    // ⭐️ [신규 추가] 임무 할당 버튼을 누르면 실행되는 API
    @PostMapping("/dispatch")
    public String dispatchJob() {
        boolean success = ohtSimulator.assignJob();
        return success ? "SUCCESS" : "FAIL";
    }

}
