package client;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class MainFrame extends JFrame {

    // CardLayout과 카드를 담을 메인 패널 선언
    private CardLayout cardLayout;
    private JPanel mainPanel; // 모든 "화면" (카드)을 담을 패널

    private Socket socket;  // 모든 패널이 공유해야 할 소켓
    private String nickname;

    private LobbyPanel lobbyPanel; // 인스턴스를 필드에 저장
    private WaitingPanel waitingPanel;

    // 각 화면의 이름을 상수로 정의
    public static final String LOGIN_PANEL = "client.LoginPanel";
    public static final String LOBBY_PANEL = "client.LobbyPanel";
    public static final String CREATE_GAME_PANEL = "client.CreateGamePanel";
    public static final String WAITING_PANEL = "client.WaitingPanel";
    public static final String GAME_PANEL = "client.GamePanel";

    public MainFrame() {
        setTitle("Wolf Mafia");
        setSize(800, 500);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 화면 중앙에

        // CardLayout과 mainPanel 초기화
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 각 화면(JPanel) 생성, MainFrame을 넘기도록 수정. 서버와의 통신을 위함
        JPanel loginPanel = new LoginPanel(this);
        this.lobbyPanel = new LobbyPanel(this);
        JPanel createGamePanel = new CreateGamePanel(this);
        this.waitingPanel = new WaitingPanel(this);
        JPanel gamePanel = new GamePanel(this);

        // mainPanel에 각 화면을 "이름"과 함께 추가
        mainPanel.add(loginPanel, LOGIN_PANEL);
        mainPanel.add(lobbyPanel, LOBBY_PANEL);
        mainPanel.add(createGamePanel, CREATE_GAME_PANEL);
        mainPanel.add(this.waitingPanel, WAITING_PANEL);
        mainPanel.add(gamePanel, GAME_PANEL);

        // 프레임에 mainPanel 추가
        add(mainPanel);
        cardLayout.show(mainPanel, LOGIN_PANEL);    // 처음 보여줄 화면 설정
    }

    // 1. 화면 전환 메소드(자식들이 호출해 사용)
    public void changePanel(String panelName) {
        cardLayout.show(mainPanel, panelName);
    }

    // 2. 소켓 저장 메소드
    public void setSocket(Socket socket, String nickname) {
        this.socket = socket;
        this.nickname = nickname;
    }

    // 3. 소켓, 닉네임 가져오기
    public Socket getSocket() {
        return this.socket;
    }
    public String getNickname() {
        return this.nickname;
    }

    // 접속 성공 시, 소켓 저장 + 수신 스레드 시작 + 화면 전환
    public void connectSuccess(Socket socket, String nickname) {
        System.out.println("접속 유저: " + nickname);
        setSocket(socket, nickname); // 소켓과 닉네임을 메인프레임에 저장

        try {
            // 서버로부터 읽어올 스트림 생성
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // 수신 전담 스레드 생성 및 시작 -> 서버의 말을 듣기 시작
            ClientReceiver receiver = new ClientReceiver(this, in);
            new Thread(receiver).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 로비 화면으로 이동
        cardLayout.show(mainPanel, LOBBY_PANEL);
    }

    public void handleServerMessage(String message) {
        System.out.println("[Client] <수신> "+ message);
        // 모든 UI 업데이트는 Swing의 이벤트 디스패치 스레드에서 처리해야 안전함
        SwingUtilities.invokeLater(() -> {

            // === 방 입장/생성 관련 처리 ===
            if (message.equals("join_ok") || message.equals("create_ok")) {
                // 방 입장 성공 -> 게임 화면(대기방)으로
                changePanel(GAME_PANEL);
            }
            else if (message.startsWith("join_fail")) {
                // 방 입장 실패 -> 경고창
                JOptionPane.showMessageDialog(this, "입장 실패: " + message.substring(11));
            }
            // === 대기방 관련 처리 ===
            // 1. 유저 목록 업데이트 처리 (/userlist 닉1 닉2 ...)
            else if (message.startsWith("/userlist ")) {
                String userListString = message.substring(10).trim();
                // 공백 기준으로 닉네임 분리
                String[] users = userListString.isEmpty() ? new String[0] : userListString.split(" ");
                waitingPanel.updateUserList(users);
                
            }
            // === 로비 관련 처리 ===
            // 서버가 "/roomlist 방1, 방2, ..." 형식으로 보낸다고 가정
            else if (message.startsWith("/roomlist ")) {
                // todo lobbyPanel.updateRoomList() 호출 구현 필요
            }
            // === 채팅 처리 ===
            // 채팅 및 시스템 메시지 처리 (나머지는 EnterGamePanel의 채팅창으로 보냄)
            else {
                // todo enterGamePanel 정의. enterGamePanel은 게임 참가 눌렀을 떄 나오는 패널. 대기 방이 아님.
                waitingPanel.appendMessage(message);
            }
        });
    }
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }
}