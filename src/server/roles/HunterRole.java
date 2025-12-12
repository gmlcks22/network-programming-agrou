package server.roles;

import server.GameRoom;

public class HunterRole implements Role {

    @Override
    public String getName() {
        return "사냥꾼";
    }

    @Override
    public String getFaction() {
        return "Citizen";
    }

    @Override
    public String useNightAbility(String targetNickname, GameRoom room) {
        // 사냥꾼은 밤에 사용하는 능력이 따로 없습니다. (죽을 때만 발동)
        return "사냥꾼은 밤에 능력을 사용할 수 없습니다.";
    }
}
