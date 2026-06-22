package dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OhtData {
    private String ohtId;
    private double x;
    private double y;
    private String status;
    private int targetNodeIndex;    // 당장 향하고 있는 바로 다음 교차로
    private int finalTargetNode;    // ⭐️ 최종 목적지 (장비 또는 창고)
    private String jobState;
    private boolean hasWafer;
    private int waitTimer;
    private int battery; 
}
