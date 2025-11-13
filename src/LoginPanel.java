// LoginPanel.java (새 클래스 파일)
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// 1. "extends JPanel"을 통해 이 클래스 자체가 JPanel이 됩니다.
public class LoginPanel extends JPanel {

    // 2. 화면 전환에 필요한 부모(mainPanel)와 CardLayout을 받아옴
    private JPanel mainPanel;
    private CardLayout cardLayout;

    // 3. (중요) 생성자(Constructor)
    //    createLoginPanel() 메소드의 내용이 이 안으로 들어옵니다.
    //    화면 전환을 위해 MainFrame으로부터 mainPanel과 cardLayout을 전달받습니다.
    public LoginPanel(JPanel mainPanel, CardLayout cardLayout) {
        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;

        // 4. "panel.add"가 아니라 "this.add" (또는 그냥 add)
        //    (이 클래스 자체가 JPanel이니까!)
        this.setBackground(Color.LIGHT_GRAY);
        this.add(new JLabel("접속 화면"));

        JButton loginButton = new JButton("접속하기");
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