package client;

import java.io.BufferedReader;
import java.io.IOException;

// 서버로부터 메시지를 지속적으로 수신하는 전용 스레드
public class ClientReceiver implements Runnable {
    private MainFrame mainFrame;
    private BufferedReader in;

    public ClientReceiver(MainFrame mainFrame, BufferedReader in) {
        this.mainFrame = mainFrame;
        this.in = in;
    }

    @Override
    public void run() {
        String message;
        try {
            // 서버에서 연결을 끊거나 스트림이 닫힐 때까지 계속 읽음
            while ((message = in.readLine()) != null) {
                // MainFrame의 중앙 처리 메소드를 호출하여 메시지를 전달
                mainFrame.handleServerMessage(message); 
            }
        } catch (IOException e) {
            // 강제 종료 또는 서버 다운 시 발생
            System.err.println("수신 스레드 종료: " + e.getMessage());
        } finally {
            // TODO: 접속 종료 처리
            System.out.println("서버 연결 수신 스레드 종료");
        }
    }
}