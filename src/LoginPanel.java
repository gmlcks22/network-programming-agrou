import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginPanel extends JPanel {

    // 화면 전환에 필요한 부모(mainPanel)와 CardLayout을 받아옴
    private JPanel mainPanel;
    private CardLayout cardLayout;

    public LoginPanel(JPanel mainPanel, CardLayout cardLayout) {
        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;

        this.setBackground(Color.LIGHT_GRAY);
        this.add(new JLabel("접속 화면"));

        JButton loginButton = new JButton("접속하기");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cardLayout.show(mainPanel, MainFrame.LOBBY_PANEL); // "LOBBY" 이름은 MainFrame과 통일
            }
        });
        this.add(loginButton);
    }
}