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

    // 연인 관계 저장 (Key: 유저, Value: 파트너)
    private Map<String, String> lovers = new ConcurrentHashMap<>();

    private int dayNumber = 0;

    // 게임 종료 중복 처리를 막기 위한 플래그
    private boolean isGameEnded = false;

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

    // 날짜 증가 및 조회 메소드 (GameEngine에서 호출)
    public void incrementDay() {
        this.dayNumber++;
        broadcastMessage("===== [ " + dayNumber + "일차 아침이 밝았습니다 ] =====");
    }

    public int getDayNumber() {
        return dayNumber;
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
            // 남은 사람이 없으면 방 폭파 (RoomManager에게 삭제 요청)
            Server.ROOM_MANAGER.removeRoom(this);
        } else {
            // 방장이 나가면 위임
            if (handler == creator && !clientsInRoom.isEmpty()) {
                creator = clientsInRoom.get(0);
                broadcastMessage("[System] 방장이 " + creator.getNickname() + "님으로 변경되었습니다.");
            }
            broadcastMessage("[System] '" + handler.getNickname() + "' 님이 방을 나갔습니다.");
            broadcastUserList();

            // 게임 중이고, 아직 종료되지 않았을 때만 승리 조건 체크
            if (isPlaying && !isGameEnded) {
                // 나간 사람을 'DISCONNECT' 원인으로 사망 처리 (이 과정에서 승리 체크됨)
                killUser(handler.getNickname(), "DISCONNECT");
            }
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

    // 마피아 채팅
    public void broadcastMafiaMessage(String message) {
        System.out.println("'" + roomName + "' (마피아챗): " + message);
        for (ClientHandler client : clientsInRoom) {
            // 역할이 있고, 진영이 Mafia인 사람에게만 전송
            if (client.getRole() != null && "Mafia".equals(client.getRole().getFaction())) {
                client.sendMessage(message);
            }
        }
    }

    // 연인 설정 메소드
    public synchronized boolean setLovers(String user1Name, String user2Name) {
        ClientHandler user1 = findClientByNickname(user1Name);
        ClientHandler user2 = findClientByNickname(user2Name);

        if (user1 == null || user2 == null) {
            return false;
        }

        // 양방향 매핑
        lovers.put(user1Name, user2Name);
        lovers.put(user2Name, user1Name);

        System.out.println("[GameRoom] 연인 탄생: " + user1Name + " - " + user2Name);

        // 1. 텍스트 안내 (플레이어 확인용)
        user1.sendMessage("[System] 💘 큐피드의 화살을 맞았습니다! 당신의 연인은 '" + user2Name + "' 입니다.");
        user2.sendMessage("[System] 💘 큐피드의 화살을 맞았습니다! 당신의 연인은 '" + user1Name + "' 입니다.");

        // 2. 프로토콜 전송 (클라이언트 UI 갱신용)
        // 형식: /loverassign 파트너닉네임
        user1.sendMessage(Protocol.CMD_LOVER_ASSIGN + " " + user2Name);
        user2.sendMessage(Protocol.CMD_LOVER_ASSIGN + " " + user1Name);

        return true;
    }

    // 연인 채팅
    public void broadcastLoverMessage(String senderName, String message) {
        if (!lovers.containsKey(senderName)) {
            return; // 연인이 아니면 무시
        }
        String partnerName = lovers.get(senderName);
        ClientHandler sender = findClientByNickname(senderName);
        ClientHandler partner = findClientByNickname(partnerName);

        String formattedMsg = "[연인] " + senderName + ": " + message;

        if (sender != null) {
            sender.sendMessage(formattedMsg);
        }
        if (partner != null) {
            partner.sendMessage(formattedMsg);
        }

        System.out.println("'" + roomName + "' (연인챗): " + message);
    }

    public void broadcastDeadMessage(String message) {
        System.out.println("'" + roomName + "' (유령챗): " + message);
        for (ClientHandler client : clientsInRoom) {
            // 사망자(isDead = true)에게만 전송
            if (client.isDead()) {
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
    public int processDayVoting() {

        if (dayVotes.isEmpty()) {
            broadcastMessage("[System] 투표가 없어 아무도 처형되지 않았습니다.");
            dayVotes.clear();
            return 0;
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

        dayVotes.clear(); // 투표함 비우기

        if (maxTarget != null && !isTie) {
            broadcastMessage("[System] 투표 결과, '" + maxTarget + "' 님이 최다 득표로 처형됩니다.");

            // 처형 대상자 객체 찾기
            ClientHandler victim = findClientByNickname(maxTarget);
            boolean isHunter = (victim != null && "사냥꾼".equals(victim.getRoleName()));

            // 유저 사망 처리
            boolean gameEnded = killUser(maxTarget, "VOTE");

            if (gameEnded) {
                return 1; // 게임 종료
            }
            // ★ 게임이 안 끝났는데 죽은 사람이 사냥꾼이라면?
            if (isHunter) {
                return 2; // 사냥꾼 이벤트 발생 신호
            }

            return 0; // 일반 진행
        } else {
            broadcastMessage("[System] 동점표가 발생하여 아무도 처형되지 않았습니다.");
            return 0;
        }
    }

    // 독재자 쿠데타 요청 (ClientHandler -> GameRoom -> GameEngine)
    public void dictatorCoup(ClientHandler dictator, String targetName) {
        // 여기서도 페이즈 체크를 한 번 더 하면 좋음
        // (GameEngine이 private이라 직접 페이즈 체크가 어려우면 생략 가능하지만 안전장치 권장)

        gameEngine.triggerDictatorCoup(dictator.getNickname(), targetName);
    }

    // 3. 유저 사망 처리
    public boolean killUser(String targetNickname) {
        return killUser(targetNickname, "GENERAL");
    }

    public boolean killUser(String targetNickname, String cause) {
        ClientHandler victim = findClientByNickname(targetNickname);

        if (victim != null && !victim.isDead()) {
            // 천사 승리 조건 체크 (죽기 전에 처리)
            // 1. 사망 원인이 투표(VOTE) 인가?
            // 2. 현재가 1일차 인가?
            // 3. 대상의 직업이 천사 인가?
            if ("VOTE".equals(cause) && dayNumber == 1 && "천사".equals(victim.getRoleName())) {
                broadcastMessage("[System] " + targetNickname + " 님은 천사였습니다! 하늘로 승천합니다.");
                endGame("천사 팀 승리! (첫날 투표로 처형당하여 승리했습니다)");
                return true; // 게임 종료
            }
            victim.setDead(true);

            // 메시지 처리
            if ("HEARTBREAK".equals(cause)) {
                broadcastMessage("[System] 비극적인 사랑! '" + targetNickname + "' 님이 연인을 따라 스스로 목숨을 끊었습니다.");
            } else if ("HUNTER".equals(cause)) {
                broadcastMessage("[System] 탕! 사냥꾼의 총에 맞아 '" + targetNickname + "' 님이 사망했습니다.");
            } else if ("DICTATOR".equals(cause)) {
                broadcastMessage("[System] '" + targetNickname + "' 님이 독재자에 의해 처형되었습니다.");
            } else if ("VOTE".equals(cause)) {
                broadcastMessage("[System] 투표 결과, '" + targetNickname + "' 님이 처형되었습니다.");
            } else {
                broadcastMessage("[System] '" + targetNickname + "' 님이 사망했습니다.");
            }

            victim.sendMessage("[System] 당신은 사망했습니다...");
            broadcastMessage(Protocol.CMD_DEATH + " " + targetNickname);

            // 연인 동반 사망 체크
            if (!"HEARTBREAK".equals(cause) && lovers.containsKey(targetNickname)) {
                String partnerName = lovers.get(targetNickname);
                ClientHandler partner = findClientByNickname(partnerName);
                if (partner != null && !partner.isDead()) {
                    killUser(partnerName, "HEARTBREAK");
                }
            }

            // 사냥꾼 능력 발동 체크 
            // (상사병으로 죽은 게 아니고, 직접 처형/살해 당했을 때만 발동)
            if (!"HEARTBREAK".equals(cause) && "사냥꾼".equals(victim.getRoleName())) {
                broadcastMessage("[System] 사냥꾼이 마지막 힘을 짜내어 총을 겨눕니다...");
                victim.sendMessage(Protocol.CMD_HUNTER_TURN); // 사냥꾼에게만 발포 기회 전송
                // 주의: 사냥꾼이 쏘기 전까지 게임이 끝나지 않도록, checkWinCondition을 
                // 사냥꾼 발포 후에 한 번 더 체크해야 할 수도 있음.
            }

            return checkWinCondition();
        }
        return false;
    }

    // 사냥꾼 발포 처리
    public synchronized void processHunterShot(ClientHandler hunter, String targetName) {
        // 이미 죽었지만(isDead=true), 사냥꾼 로직을 위해 잠시 허용됨
        if (!"사냥꾼".equals(hunter.getRoleName())) {
            return;
        }

        broadcastMessage("[System] 탕! 사냥꾼 '" + hunter.getNickname() + "' 님이 마지막 힘으로 '" + targetName + "' 님을 쏘았습니다!");

        boolean gameEnded = killUser(targetName, "HUNTER");

        if (!gameEnded) {
            // 게임이 안 끝났으면 밤으로 강제 이동
            finishHunterPhase();
        }
    }

    public void finishHunterPhase() {
        // GameEngine에게 다음 단계(밤)로 가라고 지시
        if (gameEngine != null) {
            gameEngine.resumeAfterHunter();
        }
    }

    // 승리 조건 판단 (연인 승리 추가)
    public boolean checkWinCondition() {
        int aliveCount = 0;
        int wolfCount = 0;
        int citizenCount = 0;

        // 생존자 집계
        for (ClientHandler client : clientsInRoom) {
            if (!client.isDead()) {
                aliveCount++;
                if (client.getRole().getFaction().equals("Mafia")) {
                    wolfCount++;
                } else {
                    citizenCount++;
                }
            }
        }

        // 1. 연인 승리 체크 (단 둘만 남았고, 그 둘이 연인일 때)
        if (aliveCount == 2) {
            // 살아있는 사람 찾기
            ClientHandler[] survivors = new ClientHandler[2];
            int idx = 0;
            for (ClientHandler c : clientsInRoom) {
                if (!c.isDead()) {
                    survivors[idx++] = c;
                }
            }

            // 둘이 연인 관계인지 확인
            if (lovers.containsKey(survivors[0].getNickname())
                    && lovers.get(survivors[0].getNickname()).equals(survivors[1].getNickname())) {

                endGame("사랑의 힘! 연인 팀(" + survivors[0].getNickname() + ", " + survivors[1].getNickname() + ") 승리!");
                return true;
            }
        }

        // 2. 기존 승리 조건
        if (wolfCount == 0) {
            endGame("시민 팀 승리! (모든 늑대를 처형했습니다)");
            return true;
        } else if (wolfCount >= citizenCount) {
            endGame("늑대 팀 승리! (늑대가 시민 수와 같거나 많아졌습니다)");
            return true;
        }

        return false;
    }

    // 5. 게임 종료 처리
    private void endGame(String resultMsg) {
        if (isGameEnded) {
            return; // 이미 종료 처리 중이면 무시

        }
        isGameEnded = true;      // 종료 플래그 설정
        isPlaying = false;       // 게임 중 상태 해제

        broadcastMessage("=================================");
        broadcastMessage("[GAME OVER] " + resultMsg);
        broadcastMessage("=================================");

        // 클라이언트에게 게임 종료 신호 전송
        broadcastMessage(Protocol.CMD_GAMEOVER + " " + resultMsg);

        if (gameEngine != null) {
            gameEngine.stopEngine();
        }

        // 방에 있는 모든 유저의 '현재 방' 정보를 null로 초기화 (방에서 내보냄)
        for (ClientHandler client : clientsInRoom) {
            client.setCurrentRoom(null);
        }

        // 리스트 비우기
        clientsInRoom.clear();

        // 서버의 방 목록 관리자(RoomManager)에서 이 방을 삭제
        Server.ROOM_MANAGER.removeRoom(this);

        System.out.println("[Server] 게임 종료로 인해 '" + roomName + "' 방이 삭제되었습니다.");
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
