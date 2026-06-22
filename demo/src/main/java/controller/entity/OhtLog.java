package controller.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class OhtLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 로그 고유 번호 (자동 증가)

    private String ohtId;
    private double x;
    private double y;
    private String status;
    private LocalDateTime timestamp; // 로그가 저장된 시간

    // 생성자
    public OhtLog(String ohtId, double x, double y, String status) {
        this.ohtId = ohtId;
        this.x = x;
        this.y = y;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }
}
