package common;

public class Protocol {

    // 명령어와 인자를 구분하는 구분자
    public static final String DELIMITER = " ";

    // --- 클라이언트 -> 서버 요청 명령어 ---
    public static final String REQ_NICKNAME = "/nickname";
    public static final String REQ_CREATE_ROOM = "/create";
    public static final String REQ_JOIN_ROOM = "/join";
    public static final String REQ_CHAT = "/chat";
    public static final String REQ_ROOM_LIST = "/roomlist"; // 방 목록 요청

    // --- 서버 -> 클라이언트 응답/명령어 ---
    public static final String RES_NICKNAME_OK = "nickname_ok";
    public static final String RES_NICKNAME_FAIL = "nickname_fail";

    public static final String RES_CREATE_OK = "create_ok";
    public static final String RES_CREATE_FAIL = "create_fail";

    public static final String RES_JOIN_OK = "join_ok";
    public static final String RES_JOIN_FAIL = "join_fail";

    // 서버가 클라이언트에게 일방적으로 보내는 정보 (Broadcast)
    public static final String BROADCAST_CHAT = "/chat"; // 채팅 메시지 전달
    public static final String BROADCAST_USER_LIST = "/userlist"; // 사용자 목록 업데이트
    public static final String BROADCAST_ROOM_LIST = "/roomlist"; // 방 목록 업데이트

}