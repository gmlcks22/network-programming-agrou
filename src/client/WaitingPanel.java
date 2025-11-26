package client;

import common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

public class WaitingPanel extends JPanel {
    private MainFrame mainFrame;
    private JTextArea chatArea; 
    private JTextField chatField; 
    
    // 유저 목록 표시를 위한 JList와 모델
    private JList<String> userList;
    private DefaultListModel<String> userListModel; 

    public WaitingPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(240, 240, 255));

        // 1. 상단: 방 제목 표시
        JLabel titleLabel = new JLabel("게임 대기실 (공개방)");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(titleLabel, BorderLayout.NORTH);

        // 2. 중앙 레이아웃 분할 (채팅창과 유저 목록)
        JPanel centerPanel = new JPanel(new BorderLayout(10, 0));
        
        // 왼쪽: 채팅창
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // 오른쪽: 유저 목록 (JList)
        userListModel = new DefaultListModel<>(); // 모델 초기화
        userList = new JList<>(userListModel);
        userList.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 400)); // 너비 고정
        centerPanel.add(userScrollPane, BorderLayout.EAST); // 우측에 유저 리스트 배치
        
        add(centerPanel, BorderLayout.CENTER); // 메인 패널의 중앙에 추가

        // 3. 하단: 채팅 입력 및 나가기 버튼
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        chatField = new JTextField();
        chatField.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        
        JButton exitButton = new JButton("나가기");
        exitButton.setBackground(new Color(255, 100, 100));
        exitButton.setForeground(Color.WHITE);

        bottomPanel.add(chatField, BorderLayout.CENTER);
        bottomPanel.add(exitButton, BorderLayout.EAST);
        
        add(bottomPanel, BorderLayout.SOUTH);

        // --- 이벤트 리스너 ---
        
        chatField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String msg = chatField.getText();
                if(!msg.isEmpty()){
                    // 서버로 전송, 쓴 글은 서버가 broadcast 해줄 때 화면에 나옴
                    try {
                        if (mainFrame.getSocket() != null) {
                            PrintWriter out = new PrintWriter(mainFrame.getSocket().getOutputStream(), true);
                            out.println(Protocol.CMD_CHAT + " " + msg);
                            chatField.setText("");
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
//                    appendMessage(mainFrame.getNickname() + ": " + msg);
//                    chatField.setText("");
                }
            }
        });

        exitButton.addActionListener(e -> {
            try {
                if (mainFrame.getSocket() != null) {
                    PrintWriter out = new PrintWriter(mainFrame.getSocket().getOutputStream(), true);
                    out.println(Protocol.CMD_LEAVE);
                }
                // 대기방 내용 초기화
                chatArea.setText("");
                userListModel.clear();
                // todo: 서버측에서 확인을 받고 나갈 필요 있음. 현재는 일단 그냥 전환되도록 해놓음
                mainFrame.changePanel(MainFrame.LOBBY_PANEL);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
    }

    // 채팅창에 글씨를 쓰는 메소드 (MainFrame이 호출)
    public void appendMessage(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }
    
    // 유저 리스트를 갱신하는 메소드 (MainFrame이 호출)
    public void updateUserList(String[] users) {
        userListModel.clear();
        for (String user : users) {
            if (!user.isEmpty()) {
            userListModel.addElement(user);
            }
        }
        userList.revalidate();
        userList.repaint();
        this.revalidate();
    }
}