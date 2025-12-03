// server/roles/GuardRole.java
package server.roles;

import server.GameRoom;

public class GuardRole implements Role {
    @Override
    public String getName() {
        return "경비병"; 
    }

    @Override
    public String getFaction() {
        return "Citizen";
    }

    @Override
    public String useNightAbility(String targetNickname, GameRoom room) {
        room.recordNightAction(getName(), targetNickname);
        return "당신은 오늘 밤 '" + targetNickname + "' 님을 지킵니다.";
    }
}