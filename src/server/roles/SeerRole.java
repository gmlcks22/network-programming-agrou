package server.roles;

import server.GameRoom;

public class SeerRole implements Role {

    @Override
    public String getName() {
        return "Seer"; // 선견자
    }

    @Override
    public String getFaction() {
        return "Citizen";
    }

    @Override
    public String useNightAbility(String targetNickname, GameRoom room) {
        // 선견자의 선택을 GameRoom에 전달합니다.
        room.recordNightAction(getName(), targetNickname);
        
        return "당신은 오늘 밤 '" + targetNickname + "' 님을 관찰합니다.";
    }
}
