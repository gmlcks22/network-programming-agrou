// server/roles/CitizenRole.java
package server.roles;

import server.GameRoom;

public class CitizenRole implements Role {

    @Override
    public String getName() {
        return "Citizen"; // 시민
    }

    @Override
    public String getFaction() {
        return "Citizen";
    }

    // 시민은 능력이 없으므로 아무것도 하지 않습니다.
    @Override
    public String useNightAbility(String targetNickname, GameRoom room) {
        return "시민은 밤에 사용할 능력이 없습니다.";
    }
}