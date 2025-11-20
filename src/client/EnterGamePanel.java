package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class EnterGamePanel extends JPanel {
    private MainFrame mainFrame;
    private JTextArea chatArea; // 채팅 내용이 뜰 곳
    private JTextField chatField; // 채팅 입력란

    public EnterGamePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 255)); // 연한 파란색 배경

        // 1. 상단: 방 제목 표시
        JLabel titleLabel = new JLabel("게임 대기실 (공개방)");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(titleLabel, BorderLayout.NORTH);

        // 2. 중앙: 채팅창 및 유저 목록
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        add(scrollPane, BorderLayout.CENTER);

        // 3. 하단: 채팅 입력 및 나가기 버튼
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        chatField = new JTextField();
        chatField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        
        JButton exitButton = new JButton("나가기");
        exitButton.setBackground(new Color(255, 100, 100)); // 빨간색
        exitButton.setForeground(Color.WHITE);

        bottomPanel.add(chatField, BorderLayout.CENTER);
        bottomPanel.add(exitButton, BorderLayout.EAST);
        
        add(bottomPanel, BorderLayout.SOUTH);

        // --- 이벤트 리스너 ---
        
        // 채팅 입력 엔터키 처리
        chatField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String msg = chatField.getText();
                if(!msg.isEmpty()){
                    // TODO: 서버로 채팅 전송 구현 필요 ("/chat " + msg)
                    appendMessage("나: " + msg);
                    chatField.setText("");
                }
            }
        });

        // 나가기 버튼 (로비로 이동)
        exitButton.addActionListener(e -> {
            // TODO: 서버에 방 나가기 요청 ("/leave") 구현 필요
            mainFrame.changePanel(MainFrame.LOBBY_PANEL);
        });
    }

    // 채팅창에 글씨를 쓰는 메소드 (외부에서 호출 가능)
    public void appendMessage(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength()); // 스크롤 자동 내림
    }
}