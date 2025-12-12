package client;

import common.Protocol;

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
    private RoomListPanel roomListPanel;

    // GamePanel을 멤버 변수(필드)로 선언
    private GamePanel gamePanel;

    // 각 화면의 이름을 상수로 정의
    public static final String LOGIN_PANEL = "client.LoginPanel";
    public static final String LOBBY_PANEL = "client.LobbyPanel";
    public static final String CREATE_GAME_PANEL = "client.CreateGamePanel";
    public static final String ROOMLIST_PANEL = "client.RoomListPanel";
    public static final String WAITING_PANEL = "client.WaitingPanel";
    public static final String GAME_PANEL = "client.GamePanel";

    public MainFrame() {
        setTitle("Wolf Mafia");
        setSize(800, 600); // 게임 화면을 고려해 크기를 조금 키움
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 화면 중앙에

        // CardLayout과 mainPanel 초기화
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 각 화면(JPanel) 생성
        JPanel loginPanel = new LoginPanel(this);
        this.lobbyPanel = new LobbyPanel(this);
        JPanel createGamePanel = new CreateGamePanel(this);
        this.waitingPanel = new WaitingPanel(this);
        this.roomListPanel = new RoomListPanel(this);
        this.gamePanel = new GamePanel(this);

        // mainPanel에 각 화면을 "이름"과 함께 추가
        mainPanel.add(loginPanel, LOGIN_PANEL);
        mainPanel.add(lobbyPanel, LOBBY_PANEL);
        mainPanel.add(createGamePanel, CREATE_GAME_PANEL);
        mainPanel.add(this.waitingPanel, WAITING_PANEL);
        mainPanel.add(this.gamePanel, GAME_PANEL);
        mainPanel.add(this.roomListPanel, ROOMLIST_PANEL);

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

    // 추가된 Getter
    public WaitingPanel getWaitingPanel() {
        return waitingPanel;
    }

    public RoomListPanel getRoomListPanel() {
        return roomListPanel;
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
        System.out.println("[Client] <수신> " + message);
        // 모든 UI 업데이트는 Swing의 이벤트 디스패치 스레드에서 처리해야 안전함
        SwingUtilities.invokeLater(() -> {

            // === 방 입장/생성 관련 처리 ===
            if (message.equals(Protocol.RESP_JOIN_OK) || message.equals(Protocol.RESP_CREATE_OK)) {
                // 방 입장 성공 -> 대기방으로
                changePanel(WAITING_PANEL);
            } else if (message.startsWith(Protocol.RESP_JOIN_FAIL)) {
                // 방 입장 실패 -> 경고창
                String reason = "";
                if (message.length() > Protocol.RESP_JOIN_FAIL.length()) {
                    reason = message.substring(Protocol.RESP_JOIN_FAIL.length() + 1);
                }
                JOptionPane.showMessageDialog(this, "입장 실패: " + reason);
            } // === 대기방 관련 처리 ===
            // 1. 유저 목록 업데이트 처리 (/userlist 닉1 닉2 ...)
            else if (message.startsWith(Protocol.CMD_USERLIST)) {
                String userListString = message.substring(10).trim();
                // 공백 기준으로 닉네임 분리
                String[] users = userListString.isEmpty() ? new String[0] : userListString.split(" ");
                waitingPanel.updateUserList(users);

                // 게임 패널에도 유저 목록 전달
                if (gamePanel != null) {
                    gamePanel.updateUserList(users);
                }
            } // 방 목록 수신 처리
            else if (message.startsWith(Protocol.CMD_ROOMLIST)) {
                String listStr = message.substring(Protocol.CMD_ROOMLIST.length() + 1).trim();
                String[] rooms = listStr.split(","); // 쉼표로 방 구분 /roomlist 방1, 방2, 방3, ...
                roomListPanel.updateRoomList(rooms);    // 패널 갱신 호출
            } // === 로비 관련 처리 ===
            // 서버가 "/roomlist 방1, 방2, ..." 형식으로 보낸다고 가정
            else if (message.startsWith(Protocol.CMD_ROOMLIST)) {
                // todo lobbyPanel.updateRoomList() 호출 구현 필요
            } // 직업 배정 알림 처리
            else if (message.startsWith(Protocol.CMD_ROLE_ASSIGN)) {
                String[] parts = message.substring(Protocol.CMD_ROLE_ASSIGN.length() + 1).split(" ");
                String roleName = parts[0];
                String faction = parts[1];

                // 1. GamePanel에 내 직업 정보 설정
                if (gamePanel != null) {
                    gamePanel.setMyRole(roleName, faction);
                }

                // 2. 화면 전환
                changePanel(GAME_PANEL);

                // 3. 사용자에게 직업 알림
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this,
                            "당신의 직업은 [" + roleName + "]이며, 진영은 [" + faction + "]입니다.",
                            "게임 시작",
                            JOptionPane.INFORMATION_MESSAGE);
                });
            } // 이번 판 등장 직업 목록 수신 처리
            else if (message.startsWith(Protocol.CMD_GAME_ROLES)) {
                String rolesStr = message.substring(Protocol.CMD_GAME_ROLES.length() + 1).trim();
                String[] roles = rolesStr.split(",");

                if (gamePanel != null) {
                    gamePanel.updateRoleBook(roles);
                }
            } // 게임 페이즈 단계 및 타이머 수신
            else if (message.startsWith(Protocol.CMD_PHASE)) {
                try {
                    // 메시지 앞뒤 공백 제거 후 분리
                    String[] parts = message.trim().split(" ");
                    // 예상 포lo맷: "/phase DAY_DISCUSSION 60"

                    if (parts.length >= 3) {
                        String phase = parts[1];
                        int time = Integer.parseInt(parts[2]); // 여기서 숫자 변환 시도

                        if (gamePanel != null) {
                            gamePanel.updatePhase(phase, time);
                        }
                    }
                } catch (NumberFormatException e) {
                    System.err.println("[클라이언트 오류] 시간 파싱 실패: " + message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else if (message.startsWith(Protocol.CMD_DEATH)) {
                String deadUser = message.substring(Protocol.CMD_DEATH.length() + 1).trim();
                if (gamePanel != null) {
                    gamePanel.handlePlayerDeath(deadUser);
                }
            } else if (message.startsWith(Protocol.CMD_LOVER_ASSIGN)) {
                // 메시지 예시: "/loverassign 홍길동"
                String partnerName = message.substring(Protocol.CMD_LOVER_ASSIGN.length() + 1).trim();

                if (gamePanel != null) {
                    // 1. 연인 채팅 콤보박스 활성화
                    gamePanel.enableLoverChat();

                    // 2. 팝업으로 한 번 더 확실하게 알려줌
                    JOptionPane.showMessageDialog(this,
                            "💘 큐피드의 화살에 맞았습니다!\n당신의 연인은 [" + partnerName + "] 님입니다.\n이제 '연인 채팅'을 사용할 수 있습니다.",
                            "사랑에 빠짐",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            } else if (message.equals(Protocol.CMD_HUNTER_TURN)) {
                if (gamePanel != null) {
                    gamePanel.enableHunterMode();
                }
            }// 게임 종료 수신
            else if (message.startsWith(Protocol.CMD_GAMEOVER)) {
                String resultMsg = message.substring(Protocol.CMD_GAMEOVER.length() + 1).trim();
                JOptionPane.showMessageDialog(this, resultMsg);

                // todo 게임 종료 페이지 추가? 일단 로비로 이동
                changePanel(LOBBY_PANEL);
            } // === 채팅 처리 ===
            // 채팅 및 시스템 메시지 처리 (나머지는 EnterGamePanel의 채팅창으로 보냄)
            else {
                waitingPanel.appendMessage(message);
                if (gamePanel != null) {
                    gamePanel.appendMessage(message);
                }
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
