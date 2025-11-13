// ClientHandler.java
import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private Socket socket;
    private BufferedReader in;  // 클라이언트로부터 메시지 수신
    private PrintWriter out;    // 클라이언트에게 메시지 송신

    private String nickname;
    private GameRoom currentRoom = null; // (중요) 내가 현재 속한 방

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

    // (중요) GameRoom이 이 ClientHandler의 클라이언트에게 메시지를 보낼 때 사용
    public void sendMessage(String message) {
        out.println(message);
    }

    @Override
    public void run() {
        try {
            // 1. 닉네임 설정
            out.println("닉네임을 입력하세요:");
            this.nickname = in.readLine();
            out.println("안녕하세요 " + nickname + "님! '기본방 (101호)'에 자동으로 참여합니다.");
            out.println("명령어: /create [방이름], /join [방이름], /chat [메시지]");

            // 2. (간소화) 로비의 "기본방"에 자동으로 참여시킴
            Server.lobby.joinRoom("기본방 (101호)", this);

            // 3. 클라이언트로부터 메시지 계속 수신
            String message;
            while ((message = in.readLine()) != null) {

                // --- 명령어 파싱 ---
                if (message.startsWith("/create ")) {
                    String roomName = message.substring(8);
                    Server.lobby.createRoom(roomName, this);

                } else if (message.startsWith("/join ")) {
                    String roomName = message.substring(6);
                    if (!Server.lobby.joinRoom(roomName, this)) {
                        sendMessage("[System] '" + roomName + "' 방을 찾을 수 없습니다.");
                    }

                } else if (message.startsWith("/chat ")) {
                    String chatMsg = message.substring(6);
                    if (currentRoom != null) {
                        // 4. (핵심) 내가 속한 방(currentRoom)에 메시지 전파를 "요청"
                        currentRoom.broadcastMessage(nickname + ": " + chatMsg);
                    } else {
                        sendMessage("[System] 방에 먼저 참여해야 합니다.");
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
            // 5. (중요) 클라이언트 접속 종료 시, 방에서 나감
            if (currentRoom != null) {
                currentRoom.removeClient(this);
            }
            try { socket.close(); } catch (IOException e) {}
        }
    }
}