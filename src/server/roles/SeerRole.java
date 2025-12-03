// server/roles/SeerRole.java
package server.roles;

import server.GameRoom;

public class SeerRole implements Role {

    @Override
    public String getName() {
        return "선견자";
    }

    @Override
    public String getFaction() {
        return "Citizen";
    }

    @Override
    public String useNightAbility(String targetNickname, GameRoom room) {
        room.recordNightAction(getName(), targetNickname);
        return "당신은 오늘 밤 '" + targetNickname + "' 님을 관찰합니다.";
    }
}