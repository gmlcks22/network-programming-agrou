package server;

public enum GamePhase {
    WAITING,        // 게임 시작 전
    DAY_DISCUSSION, // 낮: 토론 시간 (채팅 가능)
    DAY_VOTE,       // 낮: 투표 시간 (누구를 죽일지)
    NIGHT_ACTION,   // 밤: 능력 사용 (마피아 살해, 의사 힐 등)
    ENDED           // 게임 종료
}