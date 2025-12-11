package server;

import common.Protocol;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class GameRoom {
    private String roomName;
    private List<ClientHandler> clientsInRoom;
    private ClientHandler creator; 
    private String customRoleConfig; 
    private Map<String, String> nightActions;
    private boolean isNight = false;

    private final GameEngine gameEngine; 

    //  생성자: customRoleConfig 추가
    public GameRoom(String roomName, String customRoleConfig) {
        this.roomName = roomName;
        this.customRoleConfig = customRoleConfig;
        
        this.clientsInRoom = new CopyOnWriteArrayList<>();
        this.nightActions = new ConcurrentHashMap<>();
        
        // GameEngine 생성
        this.gameEngine = new GameEngine(this); 
    }

    public String getRoomName() { return roomName; }
    

    public String getCustomRoleConfig() { return customRoleConfig; }

    public boolean isNight() { return isNight; }
    public void setIsNight(boolean isNight) { this.isNight = isNight; }
    public List<ClientHandler> getClientsInRoom() { return clientsInRoom; }
    public Map<String, String> getNightActions() { return nightActions; }

    // --- 클라이언트 관리 ---
    public synchronized void addClient(ClientHandler handler) {
        if (handler.getCurrentRoom() != null) {
            handler.getCurrentRoom().removeClient(handler);
        }
        
        // 방에 사람이 없으면 지금 들어오는 사람이 방장
        if (clientsInRoom.isEmpty()) {
            this.creator = handler;
        }

        clientsInRoom.add(handler);
        handler.setCurrentRoom(this);

        handler.sendMessage(Protocol.RESP_JOIN_OK);
        broadcastMessage("[System] '" + handler.getNickname() + "' 님이 방에 입장했습니다.");
        broadcastUserList();
    }

    public synchronized void removeClient(ClientHandler handler) {
        clientsInRoom.remove(handler);
        handler.setCurrentRoom(null);
        broadcastMessage("[System] '" + handler.getNickname() + "' 님이 방을 나갔습니다.");
        broadcastUserList();
    }
    
    // --- 게임 시작 요청 (ClientHandler가 호출) ---
    public synchronized void startGameRequest(ClientHandler requester) {
        // 방장만 시작 가능
        if (requester != this.creator) {
            requester.sendMessage("[System] 게임 시작은 방장만 할 수 있습니다.");
            return;
        }
        // 최소 인원 체크 (4명)
        if (clientsInRoom.size() < 4) {
            requester.sendMessage("[System] 게임을 시작하려면 최소 4명이 필요합니다.");
            return;
        }
        
        // 엔진에 시작 위임
        gameEngine.assignRolesAndStartGame();
    }

    // --- 밤 능력 기록 ---
    public synchronized void recordNightAction(String roleName, String targetNickname) {
        if (!isNight) return;
        nightActions.put(roleName, targetNickname);
        System.out.println("[GameRoom] 능력 사용: " + roleName + " -> " + targetNickname);
    }
    
    // --- GameEngine 위임 메소드 ---
    public void processNight() {
        gameEngine.processNight();
    }

    // --- 통신 ---
    public void broadcastMessage(String message) {
        System.out.println("'" + roomName + "' 방 전송: " + message);
        for (ClientHandler client : clientsInRoom) {
            client.sendMessage(message);
        }
    }
    public void broadcastMafiaMessage(String message) {
            System.out.println("'" + roomName + "' (마피아챗): " + message);
            for (ClientHandler client : clientsInRoom) {
                // 역할이 있고, 진영이 Mafia인 사람에게만 전송
                if (client.getRole() != null && "Mafia".equals(client.getRole().getFaction())) {
                    client.sendMessage(message);
                }
            }
    }
    public void broadcastUserList() {
        StringBuilder list = new StringBuilder(Protocol.CMD_USERLIST);
        for (ClientHandler client : clientsInRoom) {
            list.append(" ").append(client.getNickname());
        }
        String msg = list.toString();
        for (ClientHandler client : clientsInRoom) {
            client.sendMessage(msg);
        }
    }
}