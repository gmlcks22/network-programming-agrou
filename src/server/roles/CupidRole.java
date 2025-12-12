package server.roles;

import server.GameRoom;

public class CupidRole implements Role {

    private boolean hasUsedAbility = false; // 능력 사용 여부 (1회 제한)

    @Override
    public String getName() {
        return "큐피드";
    }

    @Override
    public String getFaction() {
        return "Citizen";
    }

    @Override
    public String useNightAbility(String targetNicknames, GameRoom room) {
        if (hasUsedAbility) {
            return "이미 능력을 사용했습니다.";
        }

        // 클라이언트에서 "유저1 유저2" 형태로 보낸다고 가정
        String[] targets = targetNicknames.split(" ");
        if (targets.length != 2) {
            return "두 명의 플레이어를 선택해야 합니다.";
        }

        String user1 = targets[0];
        String user2 = targets[1];

        // 연인 관계 설정
        if (room.setLovers(user1, user2)) {
            hasUsedAbility = true;
            return "'" + user1 + "'님과 '" + user2 + "'님을 연인으로 맺어주었습니다.";
        } else {
            return "연인 지정에 실패했습니다. (존재하지 않는 유저 등)";
        }
    }
}
