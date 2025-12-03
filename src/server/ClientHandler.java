package server;// server.ClientHandler.java
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
    public String getNickname() { return nickname; }
    public GameRoom getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(GameRoom room) { this.currentRoom = room; }
    // GameRoom이 이 ClientHandler의 클라이언트에게 메시지를 보낼 때 사용
    public void sendMessage(String message) {
        out.println(message);
    }

    public void setRole(Role role) {
        this.role = role;
    }
    public Role getRole() { return role; }
    public String getRoleName() { return role != null ? role.getName() : "None"; }
    public String getFaction() { return role != null ? role.getFaction() : "None"; }

    @Override
    public void run() {
        try {
            // 클라이언트가 보낸 닉네임을 읽음
            String requestedNickname = in.readLine();

            if (requestedNickname == null) return;  // 바로 끊긴 경우

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

                } else if (message.startsWith(Protocol.CMD_CHAT)) {
                    String chatMsg = message.substring(6);
                    if (currentRoom != null) {
                        // 4. (핵심) 내가 속한 방(currentRoom)에 메시지 전파를 "요청"
                        currentRoom.broadcastMessage(nickname + ": " + chatMsg);
                    } else {
                        sendMessage("[System] 방에 먼저 참여해야 합니다.");
                    }
                } else if (message.startsWith(Protocol.CMD_LEAVE)) {
                    if (currentRoom != null) {
                        currentRoom.removeClient(this); // 방에서 제거, 안내방송, 유저목록
                    }
                    System.out.println("[Server] " + nickname + "님이 방을 나갔습니다.");
                } else if (message.equals(Protocol.CMD_ROOMLIST)) {
                    String roomListStr = Server.ROOM_MANAGER.getRoomListString();
                    sendMessage(Protocol.CMD_ROOMLIST + " " + roomListStr); // "/roomlist 방1,방2" 전송
                }else if (message.equals(Protocol.CMD_START)) { // 게임 시작 명령 처리 확인
                     if (currentRoom != null) {
                        currentRoom.startGameRequest(this);
                     } else {
                        sendMessage("[System] 방에 입장해야 합니다.");
                     }
                }
                else if (message.startsWith(Protocol.CMD_NIGHT_ACTION)) {
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
                }
                else {
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
            try { socket.close(); } catch (IOException e) {}
        }
    }
}