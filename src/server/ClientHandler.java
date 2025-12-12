package server;

import common.Protocol;
import java.io.*;
import java.net.Socket;
import server.roles.Role;

// 서버 내부에 상주하는 유저(클라이언트)
public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;  // 클라이언트로부터 메시지 수신
    private PrintWriter out;    // 클라이언트에게 메시지 송신
    private String nickname;
    private GameRoom currentRoom = null; // 내가 현재 속한 방
    private Role role;
    private boolean isDead = false; // 생존 여부

    public ClientHandler(Socket socket) {
        try {
            this.socket = socket;
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.out = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ClientHandler가 GameRoom과 통신하기 위한 getter/setter
    public String getNickname() {
        return nickname;
    }

    public GameRoom getCurrentRoom() {
        return currentRoom;
    }

    public void setCurrentRoom(GameRoom room) {
        this.currentRoom = room;
    }

    // GameRoom이 이 ClientHandler의 클라이언트에게 메시지를 보낼 때 사용
    public void sendMessage(String message) {
        out.println(message);
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Role getRole() {
        return role;
    }

    public String getRoleName() {
        return role != null ? role.getName() : "None";
    }

    public String getFaction() {
        return role != null ? role.getFaction() : "None";
    }

    public boolean isDead() {
        return isDead;
    }

    public void setDead(boolean isDead) {
        this.isDead = isDead;
    }

    @Override
    public void run() {
        try {
            // 클라이언트가 보낸 닉네임을 읽음
            String requestedNickname = in.readLine();

            if (requestedNickname == null) {
                return;  // 바로 끊긴 경우
            }
            System.out.println("[Server] <접속요청> 닉네임: " + requestedNickname);

            // 중복 검사
            if (Server.isNicknameTaken(requestedNickname)) {
                out.println("FAIL");    // 클라이언트에게 실패 알림
                System.out.println("[Server] <거부> 중복된 닉네임; " + requestedNickname);
                return;
            } else {
                out.println("OK");  // 클라이언트에게 성공 알림
                this.nickname = requestedNickname;
                Server.addClient(this); // 전체 목록에 등록
                System.out.println("[Server] <승인>" + nickname + "님 접속 완료");
            }

            sendMessage("[Server] 환영합니다!");

            // 3. 클라이언트로부터 메시지 계속 수신
            String message;
            while ((message = in.readLine()) != null) {

                // --- 명령어 파싱 ---
                if (message.startsWith(Protocol.CMD_CREATE)) {
                    // 메시지를 방 제목과 역할 목록으로 분리
                    String content = message.substring(Protocol.CMD_CREATE.length() + 1).trim();

                    // "방제목 역할목록"을 공백 기준으로 나눔 (2개 부분으로만 분리)
                    String[] parts = content.split(" ", 2);

                    if (parts.length < 2) {
                        sendMessage("[System] 방 생성 형식이 올바르지 않습니다.");
                    } else {
                        String roomName = parts[0];
                        String roleConfig = parts[1]; // 역할 목록 문자열

                        // 수정된 createRoom 호출
                        Server.ROOM_MANAGER.createRoom(roomName, roleConfig, this);
                    }

                } else if (message.startsWith(Protocol.CMD_JOIN)) {
                    String roomName = message.substring(6);
                    if (!Server.ROOM_MANAGER.joinRoom(roomName, this)) {
                        sendMessage("[System] '" + roomName + "' 방을 찾을 수 없습니다.");
                    }
                    // 시민 채팅 처리 로직
                } else if (message.startsWith(Protocol.CMD_CHAT)) {
                    // 1. 사망자 채팅 금지
                    if (isDead) {
                        sendMessage("[System] 사망자는 채팅할 수 없습니다.");
                        continue;
                    }

                    String chatMsg = message.substring(6);
                    if (currentRoom != null) {
                        // 2. 밤/낮 체크
                        if (currentRoom.isNight()) {
                            // 밤에는 마피아만 채팅 가능
                            if (role != null && "Mafia".equals(role.getFaction())) {
                                currentRoom.broadcastMafiaMessage("[마피아] " + nickname + ": " + chatMsg);
                            } else {
                                sendMessage("[System] 밤에는 채팅을 할 수 없습니다.");
                            }
                        } else {
                            // 낮에는 모두 채팅 가능
                            currentRoom.broadcastMessage(nickname + ": " + chatMsg);
                        }
                    } else {
                        sendMessage("[System] 방에 먼저 참여해야 합니다.");
                    }
                    // 마피아 채팅 처리 로직
                } else if (message.startsWith(Protocol.CMD_MAFIA_CHAT)) {
                    if (isDead) {
                        sendMessage("[System] 사망자는 채팅할 수 없습니다.");
                        continue;
                    }

                    // 마피아인지 확인
                    if (role != null && "Mafia".equals(role.getFaction())) {
                        String chatMsg = message.substring(Protocol.CMD_MAFIA_CHAT.length() + 1);
                        if (currentRoom != null) {
                            currentRoom.broadcastMafiaMessage("[마피아] " + nickname + ": " + chatMsg);
                        }
                    } else {
                        sendMessage("[System] 마피아 채팅을 사용할 수 없습니다.");
                    }

                    // 유령 채팅 처리 로직
                } else if (message.startsWith(Protocol.CMD_DEAD_CHAT)) {
                    // 유령(사망자)인지 확인
                    if (isDead) {
                        String chatMsg = message.substring(Protocol.CMD_DEAD_CHAT.length() + 1);
                        if (currentRoom != null) {
                            // GameRoom의 유령 전용 브로드캐스트 호출
                            currentRoom.broadcastDeadMessage("[유령] " + nickname + ": " + chatMsg);
                        }
                    } else {
                        sendMessage("[System] 산 사람은 유령 채팅을 볼 수 없습니다.");
                    }

                } else if (message.startsWith(Protocol.CMD_LOVER_CHAT)) {
                    if (isDead) {
                        sendMessage("[System] 사망자는 채팅할 수 없습니다.");
                        continue;
                    }
                    String chatMsg = message.substring(Protocol.CMD_LOVER_CHAT.length() + 1);
                    if (currentRoom != null) {
                        currentRoom.broadcastLoverMessage(nickname, chatMsg);
                    }
                } else if (message.startsWith(Protocol.CMD_HUNTER_SHOT)) {
                    // 사냥꾼인지 확인 (죽은 상태여도 쏠 수 있어야 함)
                    if (role != null && "사냥꾼".equals(role.getName())) {
                        String targetName = message.substring(Protocol.CMD_HUNTER_SHOT.length() + 1).trim();
                        if (currentRoom != null) {
                            currentRoom.processHunterShot(this, targetName);
                        }
                    }
                } else if (message.startsWith(Protocol.CMD_LEAVE)) {
                    if (currentRoom != null) {
                        currentRoom.removeClient(this); // 방에서 제거, 안내방송, 유저목록
                    }
                    System.out.println("[Server] " + nickname + "님이 방을 나갔습니다.");
                } else if (message.equals(Protocol.CMD_ROOMLIST)) {
                    String roomListStr = Server.ROOM_MANAGER.getRoomListString();
                    sendMessage(Protocol.CMD_ROOMLIST + " " + roomListStr); // "/roomlist 방1,방2" 전송
                } else if (message.equals(Protocol.CMD_START)) { // 게임 시작 명령 처리 확인
                    if (currentRoom != null) {
                        currentRoom.startGameRequest(this);
                    } else {
                        sendMessage("[System] 방에 입장해야 합니다.");
                    }
                } else if (message.startsWith(Protocol.CMD_NIGHT_ACTION)) {
                    if (currentRoom == null || !currentRoom.isNight()) {
                        sendMessage("[System] 지금은 능력을 사용할 수 없습니다.");
                        continue;
                    }

                    if (role == null || role.getFaction().equals("Citizen")) {
                        // 시민 역할이라도 능력이 있을 수 있으므로, 해당 역할이 'useNightAbility'를 가지고 있는지 확인
                        // 현재는 모든 직업이 Role 인터페이스를 구현했으므로, 능력이 없는 직업(시민)은 별도의 클래스로 처리 필요
                        if (role == null || role.getName().equals("Citizen")) {
                            sendMessage("[System] 당신은 능력을 사용할 수 없습니다.");
                            continue;
                        }
                    }

                    // 명령에서 대상 닉네임 추출
                    String targetNickname = message.substring(Protocol.CMD_NIGHT_ACTION.length() + 1).trim();

                    // Role 객체의 능력 사용 메소드를 호출 (GameRoom에 능력 저장)
                    String response = role.useNightAbility(targetNickname, currentRoom);

                    sendMessage("[System] " + response); // 능력 사용 성공 응답
                } else if (message.startsWith(Protocol.CMD_VOTE)) {
                    // 죽은 사람은 투표 불가
                    if (isDead) {
                        sendMessage("[System] 사망자는 투표할 수 없습니다.");
                        continue;
                    }

                    String targetName = message.substring(Protocol.CMD_VOTE.length() + 1).trim();
                    // GameRoom에 투표 처리 위임
                    if (currentRoom != null) {
                        currentRoom.castVote(this, targetName);
                    }
                } else {
                    // 기본값은 /chat으로 처리
                    if (currentRoom != null) {
                        currentRoom.broadcastMessage(nickname + ": " + message);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println(nickname + " 클라이언트 연결 끊김: " + e.getMessage());
        } finally {
            // 클라이언트 접속 종료 시, 방에서 나감
            Server.removeClient(this);  // 전체 목록에서 제거
            if (currentRoom != null) {
                currentRoom.removeClient(this); // 방에서 제거
            }
            try {
                socket.close();
            } catch (IOException e) {
            }
        }
    }
}
