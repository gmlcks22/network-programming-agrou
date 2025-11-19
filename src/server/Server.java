package server;// server.Server.java
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    public static final int PORT = 9000;

    // 1. (중요) 모든 ClientHandler가 공유할 '로비' 객체입니다.
    //    static으로 선언하여 모든 스레드가 쉽게 접근할 수 있게 합니다.
    public static final Lobby lobby = new Lobby();

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("서버가 " + PORT + " 포트에서 시작되었습니다...");

            while (true) {
                // 2. 클라이언트 접속 대기
                Socket clientSocket = serverSocket.accept();

                // 3. 새 클라이언트마다 server.ClientHandler 스레드 생성 및 시작
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                new Thread(clientHandler).start();
            }
        } catch (IOException e) {
            System.err.println("서버 오류: " + e.getMessage());
        }
    }
}