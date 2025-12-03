package server; 

import common.Protocol;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import server.roles.*;

public class GameEngine {

    private GameRoom room; 

    public GameEngine(GameRoom room) {
        this.room = room;
    }

    public void assignRolesAndStartGame() {
        List<ClientHandler> clients = room.getClientsInRoom();
        String roleConfig = room.getCustomRoleConfig(); 
        
        String[] roleNames = roleConfig.split(",");
        
        if (roleNames.length != clients.size()) {
            room.broadcastMessage("[System] 오류: 역할 수(" + roleNames.length + ")와 플레이어 수(" + clients.size() + ")가 일치하지 않습니다.");
            return;
        }

        List<Role> rolesToAssign = new LinkedList<>();
        for (String roleName : roleNames) {
            Role role = createRoleInstance(roleName.trim()); 
            if (role != null) {
                rolesToAssign.add(role);
            } else {
                room.broadcastMessage("[System] 오류: 정의되지 않은 역할(" + roleName + ")이 포함되어 있습니다.");
                return;
            }
        }
        
        Collections.shuffle(rolesToAssign); 
        
        for (int i = 0; i < clients.size(); i++) {
            ClientHandler client = clients.get(i);
            Role assignedRole = rolesToAssign.get(i);
            
            client.setRole(assignedRole);
            client.sendMessage(
                Protocol.CMD_ROLE_ASSIGN + " " + assignedRole.getName() + " " + assignedRole.getFaction()
            );
            System.out.println("[GameEngine] " + client.getNickname() + "에게 " + assignedRole.getName() + " 배정");
        }
        
        room.broadcastMessage(Protocol.CMD_GAME_ROLES + " " + roleConfig);
        
        room.setIsNight(false);
        room.broadcastMessage("--- [게임 시작] ---");
        room.broadcastMessage("[System] 직업이 배정되었습니다. 첫 번째 낮이 시작됩니다!");
    }

    public void processNight() {
        if (!room.isNight()) return;
        
        room.setIsNight(false); 
        Map<String, String> nightActions = room.getNightActions();
        
        // ... (밤 로직)
        
        nightActions.clear();
        room.broadcastMessage("[System] 이제 낮이 시작됩니다. 투표를 준비하세요.");
    }
    
    /**
     * 역할 이름에 따라 해당 Role 클래스의 인스턴스를 생성하는 헬퍼 메소드
     */
    private Role createRoleInstance(String roleName) {
        switch (roleName) {
            case "늑대인간":
                return new WolfRole(); // 내부에서 "늑대인간" 반환
            case "경비병":
                return new GuardRole(); // 내부에서 "경비병" 반환
            case "선견자":
                return new SeerRole(); // 내부에서 "선견자" 반환
            
            // 특수 직업들을 한글 이름으로 생성
            case "독재자":
                return new CitizenRole("독재자");
            case "마녀":
                return new CitizenRole("마녀");
            case "사냥꾼":
                return new CitizenRole("사냥꾼");
            case "천사":
                return new CitizenRole("천사");
            case "큐피드":
                return new CitizenRole("큐피드");
                
            case "시민":
                return new CitizenRole(); // 기본값 "시민"
            default:
                // 혹시 모를 예외 처리를 위해 기본 시민으로 반환하거나 null
                return new CitizenRole("시민"); 
        }
    }
}