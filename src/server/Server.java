package server;// server.Server.java
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

public class Server {
    public static final int PORT = 9000;

    public static final RoomManager ROOM_MANAGER = new RoomManager();

    // 현재 접속한 모든 클라이언트 핸들러를 관리하는 리스트
    private static Vector<ClientHandler> allClients = new Vector<>();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("서버가 " + PORT + " 포트에서 시작되었습니다...");
            while (true) {
                // 클라이언트 접속 대기
                Socket socket = serverSocket.accept();

                // 새 클라이언트마다 server.ClientHandler 스레드 생성 및 시작
                new Thread(new ClientHandler(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 닉네임 중복 검사
    public static synchronized boolean isNicknameTaken(String nickname) {
        for (ClientHandler client : allClients) {
            // 이미 닉네임이 있으면
            if (client.getNickname() != null && client.getNickname().equals(nickname)) {
                return true;
            }
        }
        return false;   // 사용 가능
    }

    // 접속자 명단에 추가
    public static synchronized void addClient(ClientHandler client) {
        allClients.add(client);
    }

    // 접속자 명단에서 제거
    public static synchronized void removeClient(ClientHandler client) {
        allClients.remove(client);
    }
}