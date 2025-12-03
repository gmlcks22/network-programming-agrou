// server/engine/GameEngine.java
package server;

import server.ClientHandler;
import server.GameRoom;
import server.roles.*;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GameEngine {

    private GameRoom room; // 현재 게임 룸 참조

    public GameEngine(GameRoom room) {
        this.room = room;
    }

    /**
     * 1. 인원수에 맞춰 직업을 랜덤 배정하고 게임을 시작합니다.
     */
    public void assignRolesAndStartGame() {
        List<ClientHandler> clients = room.getClientsInRoom();
        if (clients.size() < 4) {
            room.broadcastMessage("[System] 게임을 시작하려면 최소 4명이 필요합니다.");
            return;
        }

        List<Role> rolesToAssign = generateRoleList(clients.size());
        Collections.shuffle(rolesToAssign);

        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).setRole(rolesToAssign.get(i));
            // ... (직업 배정 알림 로직)
        }

        room.broadcastMessage("--- [게임 시작] ---");
        // ... (게임 시작 알림)
    }

    /**
     * 2. 밤 능력을 취합하여 결과를 처리합니다. (GameRoom의 로직 이동)
     */
    public void processNight() {
        Map<String, String> nightActions = room.getNightActions();
        
        // 늑대/경비 상호작용 처리, 선견자 정보 제공, 사망자 처리 등 
        // 게임의 모든 규칙 로직을 여기에 구현합니다.

        // ...
        nightActions.clear();
    }
    
    /**
     * 인원수에 따른 직업 목록을 생성하는 헬퍼 메소드 (GameRoom의 로직 이동)
     */
    private List<Role> generateRoleList(int population) {
        List<Role> roles = new LinkedList<>();

        int mafiaCount = 0;
        int citizenCount = 0;
        
        // 최대 늑대인간 수
        final int MAX_WOLF = population / 3;

        // --- 1. 필수 역할 배치 ---
    
        // 늑대인간 1명 필수
        if (population >= 4) {
        roles.add(new WolfRole()); 
        mafiaCount++;
        }
        
        // 선견자, 경비병 필수
        if (population >= 4) {
            roles.add(new SeerRole());
            roles.add(new GuardRole());
            citizenCount += 2;
        }

        List<Role> remainingRoles = new LinkedList<>();
    
    // 예시: 늑대인간 추가 조건 (7명 이상일 때 2마리)
    if (population >= 7 && mafiaCount < MAX_WOLF) {
        remainingRoles.add(new WolfRole());
        mafiaCount++;
    }

    // 남은 인원 (현재까지 배정된 역할 수 제외)
    int remainingPopulation = population - roles.size();
    
    // 나머지 인원을 채우기
    for (int i = 0; i < remainingPopulation; i++) {
        // 늑대인간 최대 수를 넘지 않았다면, 늑대인간을 추가할 기회를 줄 수 있지만,
        // 여기서는 나머지를 모두 시민으로 채워 밸런스를 맞춥니다.
        
        roles.add(new CitizenRole());
        citizenCount++;
    }
        return roles;
    }

    // ... (향후 낮 투표 처리, 승리 조건 확인 로직 추가)
}