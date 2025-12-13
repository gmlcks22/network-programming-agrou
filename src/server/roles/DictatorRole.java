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
        // 1. 이미 능력을 사용했는지 확인
        if (hasUsedAbility) {
            return "이미 쿠데타를 일으켰으므로 다시 사용할 수 없습니다.";
        }

        // 2. 타겟 유효성 검사 (자기 자신 지목 불가 등)
        // (ClientHandler 정보가 Role에는 없으므로, 로직은 Engine에서 최종 처리하지만
        //  여기서는 간단히 예약만 수행)

        // 3. 행동 예약 (서버의 밤 행동 맵에 저장)
        room.recordNightAction("독재자", targetNickname);

        // 4. 사용 처리
        hasUsedAbility = true;

        return "쿠데타를 준비합니다. 내일 아침 '" + targetNickname + "' 님을 단죄대에 세웁니다.";
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
