// server/GameRoom.java
package server;

import common.Protocol;
import server.roles.Role;
import server.GameEngine; // ★ GameEngine 추가

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// 하나의 게임 세션(방) - 통신 및 환경 관리 담당
public class GameRoom {
    private String roomName;

    // 이 방에 속한 server.ClientHandler(클라이언트)들의 목록
    private List<ClientHandler> clientsInRoom;

    // 밤 능력 요청 저장 (Key: 역할 이름(String), Value: 대상 닉네임(String))
    private Map<String, String> nightActions;
    private boolean isNight = false; // 현재 밤 상태 여부

    // ★ [추가] 게임 로직 처리를 위임할 엔진
    private final GameEngine gameEngine; 

    public GameRoom(String roomName) {
        this.roomName = roomName;
        this.clientsInRoom = new CopyOnWriteArrayList<>();
        this.nightActions = new ConcurrentHashMap<>();
        
        // ★ GameEngine 초기화 및 GameRoom 인스턴스 전달
        this.gameEngine = new GameEngine(this); 
    }

    // --- Getter/Setter (Engine에서 접근을 위해 필요) ---
    public String getRoomName() {
        return roomName;
    }

    public boolean isNight() {
        return isNight;
    }

    public void setIsNight(boolean isNight) {
        this.isNight = isNight;
    }

    public List<ClientHandler> getClientsInRoom() {
        return clientsInRoom;
    }
    
    public Map<String, String> getNightActions() {
        return nightActions;
    }

    // --- 클라이언트/방 관리 로직 (유지) ---

    // 클라이언트가 이 방에 입장할 때 호출됨
    public synchronized void addClient(ClientHandler handler) {
        if (handler.getCurrentRoom() != null) {
            handler.getCurrentRoom().removeClient(handler);
        }
        clientsInRoom.add(handler);
        handler.setCurrentRoom(this);

        handler.sendMessage(Protocol.RESP_JOIN_OK);
        broadcastMessage("[System] '" + handler.getNickname() + "' 님이 방에 입장했습니다.");
        broadcastUserList();
    }

    // 클라이언트가 이 방에서 나갈 때 호출됨
    public synchronized void removeClient(ClientHandler handler) {
        clientsInRoom.remove(handler);
        handler.setCurrentRoom(null); // 클라이언트의 소속 방 정보를 null로

        broadcastMessage("[System] '" + handler.getNickname() + "' 님이 방을 나갔습니다.");
        broadcastUserList();
    }
    
    // --- 게임 상태 및 능력 관리 로직 (유지) ---

    /**
     * 밤 능력을 저장하는 메소드 (Role 클래스에서 호출됨)
     * @param roleName 능력 사용자 역할 이름 (예: "Wolf", "Guard")
     * @param targetNickname 능력 대상 닉네임
     */
    public synchronized void recordNightAction(String roleName, String targetNickname) {
        if (!isNight) return;
        
        // 해당 역할의 최종 선택만 받습니다.
        nightActions.put(roleName, targetNickname);
        System.out.println("[GameRoom] 밤 능력 기록: " + roleName + " -> " + targetNickname);
    }
    
    /**
     * 클라이언트의 게임 시작 요청을 처리합니다.
     * @param requester 시작 버튼을 누른 ClientHandler
     */
    public synchronized void startGameRequest(ClientHandler requester) {
        // [TODO] 방장 체크 로직은 현재 생략합니다.
        
        // 최소 인원수 체크 (GameEngine에 정의된 4인 기준 사용)
        if (clientsInRoom.size() < 4) {
            requester.sendMessage("[System] 게임을 시작하려면 최소 4명이 필요합니다. (현재 " + clientsInRoom.size() + "명)");
            return;
        }
        
        // 게임 시작 로직을 GameEngine에 위임
        assignRolesAndStartGame();
    }

    /**
     * [위임] 게임 시작 시 역할 배정을 GameEngine에 위임합니다.
     */
    public synchronized void assignRolesAndStartGame() {
        gameEngine.assignRolesAndStartGame();
    }

    /**
     * [위임] 밤이 끝난 후, 모든 능력을 취합하여 결과를 GameEngine에 위임합니다.
     */
    public synchronized void processNight() {
        gameEngine.processNight();
    }


    // --- 통신 로직 (유지) ---

    // 메시지 전파 (Broadcasting)
    public void broadcastMessage(String message) {
        System.out.println("'" + roomName + "' 방에 메시지 전파: " + message);

        for (ClientHandler client : clientsInRoom) {
            client.sendMessage(message);
        }
    }

    public void broadcastUserList() {
        StringBuilder list = new StringBuilder(Protocol.CMD_USERLIST);
        for (ClientHandler client : clientsInRoom) {
            list.append(" ").append(client.getNickname());
        }

        String userListMessage = list.toString();
        for (ClientHandler client : clientsInRoom) {
            client.sendMessage(userListMessage);
        }
        System.out.println("[Server] 유저 목록 전파: " + userListMessage);
    }
}