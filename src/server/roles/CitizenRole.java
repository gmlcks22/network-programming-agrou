// server/roles/CitizenRole.java
package server.roles;

import server.GameRoom;

public class CitizenRole implements Role {

    private String roleName; // 직업 이름을 저장할 변수

    // 기본 생성자: "시민"
    public CitizenRole() {
        this.roleName = "시민";
    }

    //  이름을 지정하는 생성자 (마녀, 사냥꾼 등 특수 직업용)
    public CitizenRole(String roleName) {
        this.roleName = roleName;
    }

    @Override
    public String getName() {
        return this.roleName; // 저장된 한글 이름 반환
    }

    @Override
    public String getFaction() {
        return "Citizen";
    }

    @Override
    public String useNightAbility(String targetNickname, GameRoom room) {
        return "현재 이 직업의 밤 능력은 구현되지 않았습니다.";
    }
}