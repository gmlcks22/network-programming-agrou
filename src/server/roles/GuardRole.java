package server.roles;

import server.GameRoom;

public class GuardRole implements Role {
    @Override
    public String getName() {
        return "Guard"; // 경비병
    }

    @Override
    public String getFaction() {
        return "Citizen";
    }

    @Override
    public String useNightAbility(String targetNickname, GameRoom room) {
        // 경비병의 선택을 GameRoom에 전달합니다.
        room.recordNightAction(getName(), targetNickname);
        
        return "당신은 오늘 밤 '" + targetNickname + "' 님을 지킵니다.";
    }
}
