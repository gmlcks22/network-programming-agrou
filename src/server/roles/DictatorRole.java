package server.roles;

import server.GameRoom;

public class DictatorRole implements Role {

    private boolean hasUsedAbility = false; // 능력 사용 여부 (1회 제한)

    @Override
    public String getName() {
        return "독재자";
    }

    @Override
    public String getFaction() {
        return "Citizen";
    }

    @Override
    public String useNightAbility(String targetNickname, GameRoom room) {
        return "독재자는 밤에 능력을 사용할 수 없습니다.";
    }

    // 능력 사용 가능 여부 확인
    public boolean canUseAbility() {
        return !hasUsedAbility;
    }

    // 능력 사용 처리
    public void setAbilityUsed(boolean used) {
        this.hasUsedAbility = used;
    }
}
