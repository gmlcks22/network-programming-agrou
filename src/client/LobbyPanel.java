package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException; 
import java.io.PrintWriter;

public class LobbyPanel extends JPanel {

    // 1. 화면 전환에 필요한 부모(MainFrame)와 CardLayout을 받아옴
    private MainFrame mainPanel;

    // 2. GUI 컴포넌트 선언
    private JButton createRoomButton;
    private JButton findRoomButton;
    private JButton settingsButton;

    public LobbyPanel(MainFrame mainFrame) {

        this.mainPanel = mainFrame;

        // 3. 기본 설정 (백그라운드, 전체 레이아웃)
        this.setBackground(Color.WHITE);
        this.setLayout(new BorderLayout(10, 10)); // 전체 레이아웃 (북, 서)
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

        // 7. 버튼 크기 및 스타일 설정
        Dimension buttonSize = new Dimension(150, 40);
        Font buttonFont = new Font("SansSerif", Font.BOLD, 14);

        createRoomButton.setPreferredSize(buttonSize);
        createRoomButton.setMaximumSize(buttonSize);
        createRoomButton.setFont(buttonFont);
        createRoomButton.setBackground(new Color(220, 220, 220));

        findRoomButton.setPreferredSize(buttonSize);
        findRoomButton.setMaximumSize(buttonSize);
        findRoomButton.setFont(buttonFont);
        findRoomButton.setBackground(new Color(220, 220, 220));
        
        settingsButton.setPreferredSize(buttonSize);
        settingsButton.setMaximumSize(buttonSize);
        settingsButton.setFont(buttonFont);
        settingsButton.setBackground(new Color(220, 220, 220));

        // 8. 버튼 패널에 컴포넌트 추가
        buttonPanel.add(Box.createVerticalGlue()); 

        buttonPanel.add(createRoomButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        buttonPanel.add(findRoomButton);
        buttonPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        buttonPanel.add(settingsButton);

        // 9. 버튼 정렬 (왼쪽 정렬)
        createRoomButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        findRoomButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        settingsButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        
        // 10. 전체 LobbyPanel의 WEST (왼쪽) 영역에 버튼 패널 추가
        this.add(buttonPanel, BorderLayout.WEST);

        // 11. 버튼 액션 리스너
        
        // "게임 생성하기" 버튼
        createRoomButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("게임 생성하기 클릭 -> 게임 화면으로 전환");
                mainPanel.changePanel(MainFrame.CREATE_GAME_PANEL);
            }
        });

        // "방 찾기" 버튼 -> 입력 다이얼로그 후 서버에 참가 요청
        findRoomButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // 1. 방 번호 입력 다이얼로그 표시
                String roomName = JOptionPane.showInputDialog(mainPanel, "참여할 방 번호 또는 이름을 입력하세요:");

                if (roomName != null && !roomName.trim().isEmpty()) {
                    try {
                        // 2. 소켓 체크
                        if (mainPanel.getSocket() == null) {
                            JOptionPane.showMessageDialog(mainPanel, "서버에 연결되지 않았습니다.", "오류", JOptionPane.ERROR_MESSAGE);
                            return;
                        }

                        // 3. 서버로 "/join 방이름" 명령 전송
                        PrintWriter out = new PrintWriter(mainPanel.getSocket().getOutputStream(), true);
                        out.println("/join " + roomName.trim());
                        System.out.println("[Client] 방 참가 요청: " + roomName.trim());
                        
                        // 4. 요청 후, 일단 대기방 화면으로 전환 (서버의 응답은 별도의 리스너 스레드에서 처리 필요)
                        mainPanel.changePanel(MainFrame.WAITING_PANEL);

                    } catch (IOException ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(mainPanel, "통신 오류 발생!", "오류", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });
        
        // "설정" 버튼
        settingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("설정 클릭 (기능 미구현)");
            }
        });
    }
}