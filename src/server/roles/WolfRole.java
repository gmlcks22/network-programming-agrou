// server/roles/WolfRole.java
package server.roles;

import server.GameRoom;

public class WolfRole implements Role{
    @Override
    public String getName() {
        return "늑대인간"; 
    }

    @Override
    public String getFaction() {
        return "Mafia";
    }

    @Override
    public String useNightAbility(String targetNickname, GameRoom room) {
        room.recordNightAction(getName(), targetNickname);
        return "당신의 지목은 '" + targetNickname + "' 님입니다.";
    }
}