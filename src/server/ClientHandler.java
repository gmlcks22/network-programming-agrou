package server;// server.ClientHandler.java
import common.Protocol;

import java.io.*;
import java.net.Socket;

// 서버 내부에 상주하는 유저(클라이언트)
public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;  // 클라이언트로부터 메시지 수신
    private PrintWriter out;    // 클라이언트에게 메시지 송신
    private String nickname;
    private GameRoom currentRoom = null; // 내가 현재 속한 방

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
                    String roomName = message.substring(8);
                    Server.ROOM_MANAGER.createRoom(roomName, this);

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