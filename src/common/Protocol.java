package common;

public class Protocol {
    // 클라이언트 -> 서버 요청
    public static final String CMD_CREATE = "/create";
    public static final String CMD_JOIN = "/join";
    public static final String CMD_LEAVE = "/leave";
    public static final String CMD_START = "/start";

// 채팅 관련
    public static final String CMD_CHAT = "/chat";          // 전체 채팅
    public static final String CMD_MAFIA_CHAT = "/mafia";   // 마피아 전용
    public static final String CMD_DEAD_CHAT = "/dead";     // 유령 전용

// 게임 액션
    public static final String CMD_VOTE = "/vote";          // 낮 투표
    public static final String CMD_NIGHT_ACTION = "/nightvote"; // 밤 능력 사용

    // 서버 -> 클라이언트 응답
    public static final String RESP_JOIN_OK = "join_ok";
    public static final String RESP_CREATE_OK = "create_ok";
    public static final String RESP_JOIN_FAIL = "join_fail";

    // 시스템 알림
    public static final String CMD_USERLIST = "/userlist";
    public static final String CMD_ROOMLIST = "/roomlist";
    public static final String CMD_ROLE_ASSIGN = "/roleassign"; // 직업 배정
    public static final String CMD_GAME_ROLES = "/gameroles";   // 전체 직업 목록
    public static final String CMD_PHASE = "/phase";            // 페이즈 변경 (낮/밤/투표)
    public static final String CMD_DEATH = "/death";            // 플레이어 사망 알림
    public static final String CMD_GAMEOVER = "/gameover";      // 게임 종료
}