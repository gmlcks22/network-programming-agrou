package server;

import common.Protocol;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class RoomManager {

    private List<GameRoom> gameRooms;

    public RoomManager() {
        this.gameRooms = new CopyOnWriteArrayList<>();
        gameRooms.add(new GameRoom("기본방 (101호)", "늑대인간,경비병,선견자,시민"));
    }

    public synchronized void createRoom(String roomName, String customRoleConfig, ClientHandler creator) {

        // 전달받은 역할 설정으로 방 생성
        GameRoom newRoom = new GameRoom(roomName, customRoleConfig);
        gameRooms.add(newRoom);

        // 방장 입장 처리
        newRoom.addClient(creator);
        creator.sendMessage("[System] " + roomName + " 방이 생성되었습니다. (방장 자동 입장)");
        creator.sendMessage(Protocol.RESP_CREATE_OK);
    }

    public synchronized boolean joinRoom(String roomName, ClientHandler joiner) {
        for (GameRoom room : gameRooms) {
            if (room.getRoomName().equals(roomName)) {

                // 입장 조건 검사
                if (room.isPlaying()) {
                    joiner.sendMessage(Protocol.RESP_JOIN_FAIL + " 이미 게임이 진행 중입니다.");
                    return true; // (메시지 처리했으므로 true 반환하여 ClientHandler의 에러 중복 방지)
                }
                if (room.isFull()) {
                    joiner.sendMessage(Protocol.RESP_JOIN_FAIL + " 방이 꽉 찼습니다.");
                    return true;
                }

                room.addClient(joiner);
                return true;
            }
        }
        return false;
    }

    // [추가] 방 삭제 메소드
    public synchronized void removeRoom(GameRoom room) {
        gameRooms.remove(room);
        System.out.println("[Server] 방 삭제됨: " + room.getRoomName());
    }

    // 방 목록 보기: 현재 방 목록을 콤마(,)로 구분된 문자열로 반환
    public String getRoomListString() {
        StringBuilder sb = new StringBuilder();
        for (GameRoom room : gameRooms) {
            // 구분자 컴마(,) 사용. 방 이름 자체에 컴마가 없다고 가정.
            sb.append(room.getRoomInfoString()).append(",");
        }
        return sb.toString();
    }
}
