import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainFrame extends JFrame {

    // 1. CardLayout과 카드를 담을 메인 패널 선언
    private CardLayout cardLayout;
    private JPanel mainPanel; // 모든 "화면" (카드)을 담을 패널

    // 2. 각 화면의 "이름"을 상수로 정의 (실수 방지)
    public static final String LOGIN_PANEL = "LoginPanel";
    public static final String LOBBY_PANEL = "LobbyPanel";
    public static final String CREATE_GAME_PANEL = "CreateGamePanel";
    public static final String ENTER_GAME_PANEL = "EnterGamePanel";
    public static final String GAME_PANEL = "GamePanel";

    public MainFrame() {
        setTitle("Wolf Mafia");
        setSize(800, 500);
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // 화면 중앙에

        // 3. CardLayout과 mainPanel 초기화
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // 4. 각 화면(JPanel) 생성
        JPanel loginPanel = new LoginPanel(mainPanel, cardLayout);
        JPanel lobbyPanel = new LobbyPanel(mainPanel, cardLayout);
        JPanel gamePanel = new GamePanel(mainPanel, cardLayout);

        // 5. mainPanel에 각 화면을 "이름"과 함께 추가
        mainPanel.add(loginPanel, LOGIN_PANEL);
        mainPanel.add(lobbyPanel, LOBBY_PANEL);
        mainPanel.add(gamePanel, GAME_PANEL);

        // 6. 프레임에 mainPanel 추가
        add(mainPanel);

        // 7. (중요) 처음 보여줄 화면 설정
        cardLayout.show(mainPanel, LOGIN_PANEL);
    }

    // --- main 메소드 (프로그램 시작) ---
    public static void main(String[] args) {
        // (권장) Swing GUI는 Event Dispatch Thread(EDT)에서 실행
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainFrame().setVisible(true);
            }
        });
    }
}