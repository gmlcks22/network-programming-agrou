package server;// server.GameRoom.java
import common.Protocol;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

// 하나의 게임 세션(방)
public class GameRoom {
    private String roomName;

    // 이 방에 속한 server.ClientHandler(클라이언트)들의 목록
    private List<ClientHandler> clientsInRoom;

    public GameRoom(String roomName) {
        this.roomName = roomName;
        this.clientsInRoom = new CopyOnWriteArrayList<>();
    }

    public String getRoomName() {
        return roomName;
    }

    // 클라이언트가 이 방에 입장할 때 호출됨
    public synchronized void addClient(ClientHandler handler) {
        // 만약 클라이언트가 이전에 다른 방에 있었다면, 거기서 나옴
        if (handler.getCurrentRoom() != null) {
            handler.getCurrentRoom().removeClient(handler);
        }
        // 명단에 추가
        clientsInRoom.add(handler);
        handler.setCurrentRoom(this); // ClientHandler에게 자신이 이 방에 속했음을 알림

        // 당사자에게 입장 성공 신호 보내기
        handler.sendMessage(Protocol.RESP_JOIN_OK);
        // 입장했다는 사실을 모두에게 알림
        broadcastMessage("[System] '" + handler.getNickname() + "' 님이 방에 입장했습니다.");
        broadcastUserList();
    }

    // 클라이언트가 이 방에서 나갈 때 호출됨
    public synchronized void removeClient(ClientHandler handler) {
        clientsInRoom.remove(handler);
        handler.setCurrentRoom(null); // 클라이언트의 소속 방 정보를 null로

        // 나갔다는 사실을 방에 "남아있는" 사람들에게 알림
        broadcastMessage("[System] '" + handler.getNickname() + "' 님이 방을 나갔습니다.");
        broadcastUserList();
    }
    // 메시지 전파 (Broadcasting)
    // ClientHandler가 채팅을 치면 이 메소드가 호출됨
    public void broadcastMessage(String message) {
        System.out.println("'" + roomName + "' 방에 메시지 전파: " + message);

        // 5. 이 방(server.GameRoom)에 속한 "모든" ClientHandler들에게...
        for (ClientHandler client : clientsInRoom) {
            // 6. 각 ClientHandler가 맡은 클라이언트에게 메시지를 보내라고 명령
            client.sendMessage(message);
        }
    }
    public void broadcastUserList() {
        StringBuilder list = new StringBuilder(Protocol.CMD_USERLIST);
        for (ClientHandler client : clientsInRoom) {
            list.append(" ").append(client.getNickname());
        }
        
        String userListMessage = list.toString();
        // 이 방에 속한 모든 클라이언트에게 목록 전송
        for (ClientHandler client : clientsInRoom) {
            client.sendMessage(userListMessage);
        }
        System.out.println("[Server] 유저 목록 전파: " + userListMessage);
    }
}
