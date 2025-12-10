package server;

import common.Protocol;
import java.util.HashMap;
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

    private int maxPopulation;
    private boolean isPlaying = false;

    // 낮 투표 저장소: 투표자 닉네임 -> 지목된 대상 닉네임
    // 한 사람이 여러 번 투표하면 마지막 투표로 덮어씌워짐
    private Map<String, String> dayVotes;

    //  생성자: customRoleConfig 추가
    public GameRoom(String roomName, String customRoleConfig) {
        this.roomName = roomName;
        this.customRoleConfig = customRoleConfig;

        if (customRoleConfig != null && !customRoleConfig.isEmpty()) {
            this.maxPopulation = customRoleConfig.split(",").length;
        } else {
            this.maxPopulation = 4; // 기본값
        }

        this.clientsInRoom = new CopyOnWriteArrayList<>();
        this.nightActions = new ConcurrentHashMap<>();

        // GameEngine 생성
        this.gameEngine = new GameEngine(this);

        this.dayVotes = new ConcurrentHashMap<>();
    }

    public String getRoomName() {
        return roomName;
    }

    public String getCustomRoleConfig() {
        return customRoleConfig;
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

    public boolean isPlaying() {
        return isPlaying;
    }

    public boolean isFull() {
        return clientsInRoom.size() >= maxPopulation;
    }

    public int getMaxPopulation() {
        return maxPopulation;
    }

    public int getCurrentPopulation() {
        return clientsInRoom.size();
    }

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
        handler.setCurrentRoom(null); // 클라이언트의 소속 방 정보를 null로

        if (clientsInRoom.isEmpty()) {
            // 1. 남은 사람이 없으면 방 폭파 (RoomManager에게 삭제 요청)
            Server.ROOM_MANAGER.removeRoom(this);
        } else {
            // 2. 사람이 남아있으면 퇴장 알림 및 목록 갱신
            broadcastMessage("[System] '" + handler.getNickname() + "' 님이 방을 나갔습니다.");
            broadcastUserList();
        }
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
        this.isPlaying = true;
        // 엔진에 시작 위임
        gameEngine.assignRolesAndStartGame();
    }

    // --- 밤 능력 기록 ---
    public synchronized void recordNightAction(String roleName, String targetNickname) {
        if (!isNight) {
            return;
        }
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

    public String getRoomInfoString() {
        String state = isPlaying ? "[진행중]" : "[대기중]";
        // 예: "1번방 (3/4) [대기중]"
        return String.format("%s (%d/%d) %s", roomName, clientsInRoom.size(), maxPopulation, state);
    }

    /* ========== 투표 기능 ========== */
    // 1. 투표 행사 (ClientHandler가 호출)
    public synchronized void castVote(ClientHandler voter, String targetNickname) {
        // 게임 중이 아니거나 밤이면 투표 불가
        // (더 정교하게 하려면 GamePhase를 GameRoom도 알고 있어야 하지만, 일단 밤 여부로 체크)
        if (isNight) {
            voter.sendMessage("[System] 지금은 투표할 수 없습니다.");
            return;
        }

        // 대상이 존재하는지, 살아있는지 확인
        ClientHandler target = findClientByNickname(targetNickname);
        if (target == null) {
            voter.sendMessage("[System] 존재하지 않는 유저입니다.");
            return;
        }
        if (target.isDead()) {
            voter.sendMessage("[System] 이미 사망한 유저에게는 투표할 수 없습니다.");
            return;
        }

        // 투표 기록 (누가 누구를 찍었는지)
        dayVotes.put(voter.getNickname(), targetNickname);

        // (선택 사항) 투표 실명제: 누가 누구를 찍었는지 모두에게 알림
        broadcastMessage("[투표] '" + voter.getNickname() + "' 님이 '" + targetNickname + "' 님에게 투표했습니다.");
    }

    // 2. 투표 결과 집계 및 처형 (GameEngine이 투표 시간 종료 시 호출)
    public boolean processDayVoting() {
        boolean isGameEnded = false;    // 게임 종료 여부 플래그

        if (dayVotes.isEmpty()) {
            broadcastMessage("[System] 투표가 없어 아무도 처형되지 않았습니다.");
            return false;
        }

        // 득표수 계산
        Map<String, Integer> voteCounts = new HashMap<>();
        for (String target : dayVotes.values()) {
            voteCounts.put(target, voteCounts.getOrDefault(target, 0) + 1);
        }

        // 최다 득표자 찾기
        String maxTarget = null;
        int maxVotes = -1;
        boolean isTie = false;

        for (Map.Entry<String, Integer> entry : voteCounts.entrySet()) {
            int count = entry.getValue();
            if (count > maxVotes) {
                maxVotes = count;
                maxTarget = entry.getKey();
                isTie = false;
            } else if (count == maxVotes) {
                isTie = true; // 동점자 발생
            }
        }

        // 결과 처리
        if (maxTarget != null && !isTie) {
            broadcastMessage("[System] 투표 결과, '" + maxTarget + "' 님이 최다 득표로 처형됩니다.");
            isGameEnded = killUser(maxTarget);
        } else {
            broadcastMessage("[System] 동점표가 발생하여 아무도 처형되지 않았습니다.");
        }

        // 투표함 초기화
        dayVotes.clear();
        return isGameEnded;
    }

    // 3. 유저 사망 처리
    public boolean killUser(String targetNickname) {
        ClientHandler victim = findClientByNickname(targetNickname);
        if (victim != null && !victim.isDead()) {
            victim.setDead(true);
            victim.sendMessage("[System] 당신은 사망했습니다...");
            broadcastMessage("[System] " + targetNickname + " 님이 사망했습니다.");

            // 여기서 승리 조건을 체크하고, 그 결과를 바로 return.
            return checkWinCondition();
        }
        return false; // 아무도 안 죽었거나 에러면 게임 안 끝남
    }

    // 4. 승리 조건 판단 (GameEngine이나 killUser에서 호출)
    public boolean checkWinCondition() {
        int wolfCount = 0;
        int citizenCount = 0;

        for (ClientHandler client : clientsInRoom) {
            if (!client.isDead()) { // 살아있는 사람만 카운트
                if (client.getRole().getFaction().equals("Mafia")) {
                    wolfCount++;
                } else {
                    citizenCount++;
                }
            }
        }

        // 승리 판별
        if (wolfCount == 0) {
            endGame("시민 팀 승리! (모든 늑대를 처형했습니다)");
            return true;
        } else if (wolfCount >= citizenCount) {
            endGame("늑대 팀 승리! (늑대가 시민 수와 같거나 많아졌습니다)");
            return true;
        }

        return false; // 게임 계속 진행
    }

    // 5. 게임 종료 처리
    private void endGame(String resultMsg) {
        broadcastMessage("=================================");
        broadcastMessage("[GAME OVER] " + resultMsg);
        broadcastMessage("=================================");

        // 클라이언트에게 게임 종료 신호 전송
        broadcastMessage(Protocol.CMD_GAMEOVER + " " + resultMsg);

        // GameEngine 멈추기 (구현 필요 시 Engine에 stop 메소드 추가)
        // gameEngine.stop();
        // (선택) 게임 종료 후 방 폭파 또는 초기화 로직
    }

    // 헬퍼: 닉네임으로 객체 찾기
    private ClientHandler findClientByNickname(String nickname) {
        for (ClientHandler client : clientsInRoom) {
            if (client.getNickname().equals(nickname)) {
                return client;
            }
        }
        return null;
    }
}
