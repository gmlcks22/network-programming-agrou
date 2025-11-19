package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

// 1. "extends JPanel"을 통해 이 클래스 자체가 JPanel이 됩니다.
public class LoginPanel extends JPanel {

    // 2. 화면 전환에 필요한 부모(mainPanel)와 CardLayout을 받아옴
    private JPanel mainPanel;
    private CardLayout cardLayout;

    // 3. GUI 컴포넌트 선언
    private JTextField ipField;
    private JTextField portField;
    private JTextField nicknameField;
    private JButton loginButton;

    // 4. (중요) 생성자(Constructor)
    public LoginPanel(JPanel mainPanel, CardLayout cardLayout) {
        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;

        // 5. 기본 설정 (백그라운드, 전체 레이아웃)
        this.setBackground(Color.WHITE);
        this.setLayout(new BorderLayout(10, 10)); // 전체 레이아웃 (북, 중앙)
        // 화면 전체에 여백
        this.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // 6. 타이틀 (North)
        JLabel titleLabel = new JLabel("Mafia Game");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 40));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(titleLabel, BorderLayout.NORTH);

        // 7. 폼 패널 (Center) - GridBagLayout을 사용해 폼을 중앙에 배치
        JPanel formWrapperPanel = new JPanel(new GridBagLayout());
        formWrapperPanel.setBackground(Color.WHITE); // 배경색 통일

        // 7-1. 실제 폼 컴포넌트(필드, 버튼)들을 담을 패널 (Y축으로 쌓음)
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);

        // 7-2. 필드 및 버튼 생성
        ipField = new JTextField(20);
        portField = new JTextField(20);
        nicknameField = new JTextField(20);
        loginButton = new JButton("로그인");

        // (참고) 이미지처럼 Placeholder 텍스트 구현
        addPlaceholder(ipField, "서버 ip");
        addPlaceholder(portField, "포트번호");
        addPlaceholder(nicknameField, "닉네임");

        // 7-3. 컴포넌트 크기 설정 (일관된 크기)
        Dimension fieldSize = new Dimension(250, 40);
        ipField.setPreferredSize(fieldSize);
        ipField.setMaximumSize(fieldSize); // BoxLayout을 위해
        portField.setPreferredSize(fieldSize);
        portField.setMaximumSize(fieldSize);
        nicknameField.setPreferredSize(fieldSize);
        nicknameField.setMaximumSize(fieldSize);
        loginButton.setPreferredSize(fieldSize);
        loginButton.setMaximumSize(fieldSize);
        
        // 7-4. 폰트 및 정렬 설정
        Font fieldFont = new Font("SansSerif", Font.PLAIN, 14);
        ipField.setFont(fieldFont);
        portField.setFont(fieldFont);
        nicknameField.setFont(fieldFont);
        
        loginButton.setBackground(new Color(220, 220, 220)); // 이미지와 유사한 회색
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));

        // 7-5. 폼 패널에 컴포넌트 추가
        formPanel.add(ipField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10))); // 컴포넌트 사이 간격
        formPanel.add(portField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10))); // 간격
        formPanel.add(nicknameField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20))); // 버튼 전 간격
        formPanel.add(loginButton);

        // 7-6. 컴포넌트들 중앙 정렬 (BoxLayout)
        ipField.setAlignmentX(Component.CENTER_ALIGNMENT);
        portField.setAlignmentX(Component.CENTER_ALIGNMENT);
        nicknameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 8. formWrapperPanel(GridBagLayout)에 formPanel을 추가 (패널 자체가 중앙에 감)
        GridBagConstraints gbc = new GridBagConstraints();
        formWrapperPanel.add(formPanel, gbc); // gbc 기본값이 중앙

        // 9. 전체 LoginPanel의 CENTER에 formWrapperPanel을 추가
        this.add(formWrapperPanel, BorderLayout.CENTER);

        // 10. "로그인" 버튼 액션 리스너 (기능X, 화면 전환만)
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // (임시) 버튼 누르면 무조건 로비로 이동
                System.out.println("로그인 버튼 클릭 (화면 전환만)");
                
                // (나중에 실제 로직이 들어갈 곳)
                // String ip = ipField.getText();
                // String port = portField.getText();
                // String nickname = nicknameField.getText();
                // clientService.connect(ip, port, nickname);
                
                cardLayout.show(mainPanel, MainFrame.LOBBY_PANEL);
            }
        });
    }

    /**
     * (부가 기능) JTextField에 Placeholder 텍스트를 추가하는 헬퍼 메소드
     */
    private void addPlaceholder(JTextField textField, String placeholder) {
        textField.setText(placeholder);
        textField.setForeground(Color.GRAY);

        textField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // 클릭해서 포커스를 얻었을 때
                if (textField.getText().equals(placeholder)) {
                    textField.setText("");
                    textField.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                // 포커스를 잃었을 때
                if (textField.getText().isEmpty()) {
                    textField.setText(placeholder);
                    textField.setForeground(Color.GRAY);
                }
            }
        });
    }
}