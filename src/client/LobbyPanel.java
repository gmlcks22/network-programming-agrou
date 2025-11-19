package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LobbyPanel extends JPanel {

    // 1. 화면 전환에 필요한 부모(mainPanel)와 CardLayout을 받아옴
    private MainFrame mainPanel;

    // 2. GUI 컴포넌트 선언
    private JButton createRoomButton;
    private JButton findRoomButton;
    private JButton settingsButton;

    public LobbyPanel(MainFrame mainFrame) {
        this.mainPanel = mainPanel;

        // 3. 기본 설정 (백그라운드, 전체 레이아웃)
        this.setBackground(Color.WHITE);
        this.setLayout(new BorderLayout(10, 10)); // 전체 레이아웃 (북, 서)
        // 화면 전체에 여백
        this.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // 4. 타이틀 (North)
        JLabel titleLabel = new JLabel("Mafia Game");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 40));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(titleLabel, BorderLayout.NORTH);

        // 5. 버튼 패널 (West) - Y축으로 쌓는 BoxLayout 사용
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
        buttonPanel.setBackground(Color.WHITE); // 배경색 통일

        // 6. 버튼 생성
        createRoomButton = new JButton("게임 생성하기");
        findRoomButton = new JButton("방 찾기");
        settingsButton = new JButton("설정");

        // 7. 버튼 크기 및 스타일 설정 (회색 버튼)
        Dimension buttonSize = new Dimension(150, 40);
        Font buttonFont = new Font("SansSerif", Font.BOLD, 14);

        // '게임 생성하기' 버튼
        createRoomButton.setPreferredSize(buttonSize);
        createRoomButton.setMaximumSize(buttonSize); // BoxLayout을 위해
        createRoomButton.setFont(buttonFont);
        createRoomButton.setBackground(new Color(220, 220, 220));

        // '방 찾기' 버튼
        findRoomButton.setPreferredSize(buttonSize);
        findRoomButton.setMaximumSize(buttonSize);
        findRoomButton.setFont(buttonFont);
        findRoomButton.setBackground(new Color(220, 220, 220));
        
        // '설정' 버튼
        settingsButton.setPreferredSize(buttonSize);
        settingsButton.setMaximumSize(buttonSize);
        settingsButton.setFont(buttonFont);
        settingsButton.setBackground(new Color(220, 220, 220));

        // 8. 버튼 패널에 컴포넌트 추가
        
        // (중요) 이 "Glue"가 버튼들을 패널의 아래쪽으로 밀어냅니다.
        buttonPanel.add(Box.createVerticalGlue()); 

        buttonPanel.add(createRoomButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10))); // 버튼 사이 간격
        buttonPanel.add(findRoomButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10))); // 간격
        buttonPanel.add(settingsButton);

        // 9. 버튼 정렬 (왼쪽 정렬)
        createRoomButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        findRoomButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // 10. 전체 LobbyPanel의 WEST (왼쪽) 영역에 버튼 패널 추가
        this.add(buttonPanel, BorderLayout.WEST);

        // 11. 버튼 액션 리스너 (기능X, 화면 전환만)
        
        // "게임 생성하기" 버튼 -> GamePanel로 이동 (기존 파일의 기능 유지)
        createRoomButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("게임 생성하기 클릭 -> 게임 화면으로 전환");
                cardLayout.show(mainPanel, MainFrame.GAME_PANEL);
            }
        });

        // "방 찾기" 버튼 (임시 - 기능 없음)
        findRoomButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("방 찾기 클릭 (기능 미구현)");
                // TODO: 방 찾기 팝업 또는 패널 구현
            }
        });
        
        // "설정" 버튼 (임시 - 기능 없음)
        settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("설정 클릭 (기능 미구현)");
                // TODO: 설정 팝업 또는 패널 구현
            }
        });
    }
}