import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LobbyPanel extends JPanel {

    // 화면 전환에 필요한 부모(mainPanel)와 CardLayout을 받아옴
    private JPanel mainPanel;
    private CardLayout cardLayout;

    public LobbyPanel(JPanel mainPanel, CardLayout cardLayout) {
        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;

        this.setBackground(Color.white);
        this.add(new JLabel("Wolf Mafia Client"));

        JButton createRoomButton = new JButton("Create Game");
        createRoomButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // "GAME_PANEL" 이름의 카드를 보여줌
                // todo 게임 대기 화면으로
                System.out.println("게임 화면으로 전환");
                cardLayout.show(mainPanel, MainFrame.GAME_PANEL);
            }
        });
        this.add(createRoomButton);

        JButton enterRoonButton = new JButton("Enter Game");
        enterRoonButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("[Client] 게임 화면으로 전환");
                cardLayout.show(mainPanel, MainFrame.GAME_PANEL);
            }
        });
        this.add(enterRoonButton);

        JButton backButton = new JButton("접속 끊기");
        backButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("접속 화면으로 전환");
                cardLayout.show(mainPanel, MainFrame.LOGIN_PANEL);
            }
        });
        this.add(backButton);
    }
}
