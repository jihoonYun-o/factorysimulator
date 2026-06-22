package service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dto.OhtData;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class OhtSimulator {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final List<OhtData> ohtList = new ArrayList<>();

    // 0: Load Port, 1~3: Spine, 4~7: Eqp Zones, 8: Charging Station
    private final int[][] nodes = {
            {100, 300}, {300, 300}, {500, 300}, {700, 300}, 
            {300, 150}, {500, 150},                         
            {500, 450}, {300, 450},
            {400, 450} // ⭐️ Node 8: 충전소 (아래쪽 골목길 중간)
    };

    // 일방통행 레일 (충전소를 거쳐가도록 수정)
    private final int[][] edges = {
            {0,1}, {1,2}, {2,3}, 
            {1,4}, {4,5}, {5,2}, 
            {2,6}, {6,8}, {8,7}, {7,1}, // 6 -> 8(충전소) -> 7
            {3,6}, {4,0}         
    };

    @PostConstruct
    public void init() {
        redisTemplate.delete("OHT_CACHE");
        for (int i = 0; i < 15; i++) {
            int startNode = i % 9;
            int initialBattery = 70 + (int)(Math.random() * 30);
            ohtList.add(new OhtData("OHT-" + String.format("%03d", i + 1), 
                    nodes[startNode][0], nodes[startNode][1], "RUN", startNode, startNode, "IDLE", false, 0, initialBattery));
        }
    }

    public boolean assignJob() {
        OhtData bestOht = null;
        double minDistance = Double.MAX_VALUE;
        for (OhtData oht : ohtList) {
            if ("IDLE".equals(oht.getJobState()) && "RUN".equals(oht.getStatus()) && oht.getBattery() > 30) {
                double dist = Math.hypot(oht.getX() - nodes[0][0], oht.getY() - nodes[0][1]);
                if (dist < minDistance) {
                    minDistance = dist; bestOht = oht;
                }
            }
        }
        if (bestOht != null) {
            bestOht.setJobState("TO_LOAD"); bestOht.setFinalTargetNode(0);
            return true;
        }
        return false;
    }

    public void executeCommand(String ohtId, String command) {
        for (OhtData oht : ohtList) {
            if (oht.getOhtId().equals(ohtId)) {
                if ("STOP".equals(command)) oht.setStatus("MANUAL_STOP");
                else if ("RESUME".equals(command)) oht.setStatus("RUN");
                else if ("REPAIR".equals(command)) oht.setStatus("RUN");
                else if ("ERROR".equals(command)) oht.setStatus("ERROR");
                break;
            }
        }
    }

    private int getNextNodeDijkstra(int start, int end, Set<Integer> blockedNodes, Map<Integer, Integer> trafficCount) {
        if (start == end) return end;
        int[] dist = new int[9]; int[] prev = new int[9];
        Arrays.fill(dist, Integer.MAX_VALUE); Arrays.fill(prev, -1);
        dist[start] = 0;

        PriorityQueue<Integer> pq = new PriorityQueue<>(Comparator.comparingInt(n -> dist[n]));
        pq.add(start);

        while (!pq.isEmpty()) {
            int curr = pq.poll();
            if (curr == end) break;

            for (int[] edge : edges) {
                if (edge[0] == curr) {
                    int neighbor = edge[1];
                    if (!blockedNodes.contains(neighbor)) {
                        int baseWeight = (int) Math.hypot(nodes[curr][0] - nodes[neighbor][0], nodes[curr][1] - nodes[neighbor][1]);
                        int penalty = (trafficCount.getOrDefault(neighbor, 0) >= 2) ? 10000 : 0; 
                        if (dist[curr] + baseWeight + penalty < dist[neighbor]) {
                            dist[neighbor] = dist[curr] + baseWeight + penalty;
                            prev[neighbor] = curr; pq.add(neighbor);
                        }
                    }
                }
            }
        }
        int curr = end;
        if (prev[curr] == -1) return start; 
        while (prev[curr] != start) curr = prev[curr];
        return curr;
    }

    @Scheduled(fixedRate = 100)
    public void simulateMovement() {
        Map<Integer, Integer> trafficCount = new HashMap<>();
        Set<Integer> blockedNodes = new HashSet<>();
        
        for (OhtData oht : ohtList) {
            if ("ERROR".equals(oht.getStatus())) blockedNodes.add(oht.getTargetNodeIndex());
            else trafficCount.put(oht.getTargetNodeIndex(), trafficCount.getOrDefault(oht.getTargetNodeIndex(), 0) + 1);
        }

        for (OhtData oht : ohtList) {
            if (!"ERROR".equals(oht.getStatus()) && !"MANUAL_STOP".equals(oht.getStatus())) {
                
                // ⭐️ 배터리 소모 및 충전 로직 복구
                if (!"CHARGING".equals(oht.getJobState())) {
                    if (Math.random() < 0.03) oht.setBattery(Math.max(0, oht.getBattery() - 1));
                    if (oht.getBattery() <= 20 && "IDLE".equals(oht.getJobState())) {
                        oht.setJobState("TO_CHARGE");
                        oht.setFinalTargetNode(8); // 충전소로 목적지 변경
                    }
                }

                if (oht.getWaitTimer() > 0) {
                    oht.setWaitTimer(oht.getWaitTimer() - 1);
                    if (oht.getWaitTimer() == 0) {
                        if ("LOADING".equals(oht.getJobState())) {
                            oht.setHasWafer(true); oht.setJobState("TO_UNLOAD");
                            int[] eqpNodes = {4, 5, 6, 7};
                            oht.setFinalTargetNode(eqpNodes[(int)(Math.random() * 4)]);
                        } else if ("UNLOADING".equals(oht.getJobState())) {
                            oht.setHasWafer(false); oht.setJobState("IDLE");
                            oht.setFinalTargetNode(3);
                        }
                    }
                } else if ("CHARGING".equals(oht.getJobState())) {
                    oht.setBattery(Math.min(100, oht.getBattery() + 2));
                    if (oht.getBattery() >= 100) {
                        oht.setJobState("IDLE");
                        oht.setFinalTargetNode(1); // 완충 후 메인 레일로 복귀
                    }
                } else {
                    int targetIdx = oht.getTargetNodeIndex();
                    double targetX = nodes[targetIdx][0];
                    double targetY = nodes[targetIdx][1];

                    double dx = targetX - oht.getX();
                    double dy = targetY - oht.getY();
                    double distance = Math.sqrt(dx * dx + dy * dy);

                    boolean collisionRisk = false;
                    for (OhtData other : ohtList) {
                        if (oht == other) continue;
                        if (other.getTargetNodeIndex() == targetIdx || (Math.abs(other.getX() - targetX) < 5 && Math.abs(other.getY() - targetY) < 5)) {
                            double distToOther = Math.hypot(other.getX() - oht.getX(), other.getY() - oht.getY());
                            double otherDistToTarget = Math.hypot(targetX - other.getX(), targetY - other.getY());
                            if (distToOther < 35 && distance > otherDistToTarget) {
                                collisionRisk = true;
                                break;
                            }
                        }
                    }

                    if (collisionRisk) {
                        oht.setStatus("WAIT"); 
                    } else {
                        if (distance < 5) {
                            if (targetIdx == oht.getFinalTargetNode()) {
                                if ("TO_LOAD".equals(oht.getJobState()) && targetIdx == 0) {
                                    oht.setJobState("LOADING"); oht.setWaitTimer(20);
                                } else if ("TO_UNLOAD".equals(oht.getJobState()) && targetIdx >= 4 && targetIdx <= 7) {
                                    oht.setJobState("UNLOADING"); oht.setWaitTimer(20);
                                } else if ("TO_CHARGE".equals(oht.getJobState()) && targetIdx == 8) {
                                    oht.setJobState("CHARGING"); // 충전소 도착
                                } else if ("IDLE".equals(oht.getJobState())) {
                                    oht.setFinalTargetNode((targetIdx + 1) % 9);
                                }
                            }
                            int nextNode = getNextNodeDijkstra(targetIdx, oht.getFinalTargetNode(), blockedNodes, trafficCount);
                            if (nextNode != targetIdx) {
                                oht.setTargetNodeIndex(nextNode);
                                oht.setStatus("RUN");
                            } else {
                                oht.setStatus("WAIT");
                            }
                        } else {
                            oht.setX(oht.getX() + (dx / distance) * 8);
                            oht.setY(oht.getY() + (dy / distance) * 8);
                            oht.setStatus("RUN");
                        }
                    }
                }
            }

            try {
                String json = objectMapper.writeValueAsString(oht);
                redisTemplate.opsForHash().put("OHT_CACHE", oht.getOhtId(), json);
                redisTemplate.convertAndSend("oht-topic", json);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
