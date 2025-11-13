import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GamePanel extends JPanel {
    // 2. 화면 전환에 필요한 부모(mainPanel)와 CardLayout을 받아옴
    private JPanel mainPanel;
    private CardLayout cardLayout;

    // 3. (중요) 생성자(Constructor)
    //    createLoginPanel() 메소드의 내용이 이 안으로 들어옵니다.
    //    화면 전환을 위해 MainFrame으로부터 mainPanel과 cardLayout을 전달받습니다.
    public GamePanel(JPanel mainPanel, CardLayout cardLayout) {
        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;

        this.setBackground(Color.LIGHT_GRAY);
        this.add(new JLabel("Wolf Mafia Game"));

        JButton loginButton = new JButton("게임");
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 5. 생성자로 전달받은 cardLayout을 사용
                cardLayout.show(mainPanel, MainFrame.LOBBY_PANEL); // "LOBBY" 이름은 MainFrame과 통일
            }
        });
        this.add(loginButton);
    }
}
