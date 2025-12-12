package server.roles;

import server.GameRoom;

public class AngelRole implements Role {

    @Override
    public String getName() {
        return "천사";
    }

    @Override
    public String getFaction() {
        return "Citizen";
    }

    @Override
    public String useNightAbility(String targetNickname, GameRoom room) {
        return "천사는 밤에 능력을 사용할 수 없습니다.";
    }
}
