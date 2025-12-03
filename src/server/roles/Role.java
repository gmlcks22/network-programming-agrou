package server.roles;

public interface Role {
    // 직업의 이름을 반환합니다. (예: "Wolf", "Guard")
    String getName();

    // 직업의 진영을 반환합니다. (예: "Mafia", "Citizen")
    String getFaction();

    // 밤 능력을 사용하는 메소드.
    // targetNickname: 능력의 대상이 된 플레이어의 닉네임
    // room: 능력 처리를 위해 GameRoom 객체가 필요할 수 있음
    String useNightAbility(String targetNickname, server.GameRoom room);
}
