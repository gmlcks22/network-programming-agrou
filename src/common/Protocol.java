package common;

public class Protocol {
    // 클라이언트 -> 서버 요청
    public static final String CMD_CREATE = "/create";
    public static final String CMD_JOIN = "/join";
    public static final String CMD_CHAT = "/chat";
    public static final String CMD_LEAVE = "/leave";
    public static final String CMD_START = "/start";
    public static final String CMD_NIGHT_ACTION = "/nightvote";
    public static final String CMD_ROLE_ASSIGN = "/roleassign";
    public static final String CMD_GAME_ROLES = "/gameroles";
    // 서버 -> 클라이언트 응답
    public static final String RESP_JOIN_OK = "join_ok";
    public static final String RESP_CREATE_OK = "create_ok";
    public static final String RESP_JOIN_FAIL = "join_fail";

    // 시스템 알림
    public static final String CMD_USERLIST = "/userlist";
    public static final String CMD_ROOMLIST = "/roomlist";
}