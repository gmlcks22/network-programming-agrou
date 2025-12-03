package server.roles;

import server.GameRoom;

public class WolfRole implements Role{
    @Override
    public String getName() {
        return "Wolf"; // 늑대인간
    }

    @Override
    public String getFaction() {
        return "Mafia";
    }

    @Override
    public String useNightAbility(String targetNickname, GameRoom room) {
        // 늑대인간의 선택을 GameRoom에 전달합니다.
        // GameRoom은 모든 능력을 모아 처리할 때, 이 선택을 사용합니다.
        room.recordNightAction(getName(), targetNickname);
        
        return "당신의 지목은 '" + targetNickname + "' 님입니다.";
    }
}
