package client;

import common.Protocol;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintWriter;
import javax.swing.*;

public class WaitingPanel extends JPanel {

    private MainFrame mainFrame;
    private JTextArea chatArea;
    private JTextField chatField;

    // 유저 목록 표시를 위한 JList와 모델
    private JList<String> userList;
    private DefaultListModel<String> userListModel;

    // ★ 게임 시작 버튼 필드 추가
    private JButton startGameButton;

    public WaitingPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());

        // 1. 상단: 방 제목 표시
        JLabel titleLabel = new JLabel("게임 대기실 (공개방)");
        titleLabel.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float)24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(titleLabel, BorderLayout.NORTH);

        // 2. 중앙 레이아웃 분할 (채팅창과 유저 목록)
        JPanel centerPanel = new JPanel(new BorderLayout(10, 0));

        // 왼쪽: 채팅창
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(UIManager.getFont("defaultFont").deriveFont(Font.PLAIN, (float)14));
        JScrollPane scrollPane = new JScrollPane(chatArea);
        centerPanel.add(scrollPane, BorderLayout.CENTER);

        // 오른쪽: 유저 목록 (JList)
        userListModel = new DefaultListModel<>(); // 모델 초기화
        userList = new JList<>(userListModel);
        userList.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float)14));
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 400)); // 너비 고정
        centerPanel.add(userScrollPane, BorderLayout.EAST); // 우측에 유저 리스트 배치

        add(centerPanel, BorderLayout.CENTER); // 메인 패널의 중앙에 추가

        // 3. 하단: 채팅 입력 필드 및 버튼
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5)); // 내부 간격 5px

        chatField = new JTextField();
        chatField.setFont(UIManager.getFont("defaultFont").deriveFont(Font.PLAIN, (float)14));
        
        // ★ '게임 시작' 버튼 생성
        startGameButton = new JButton("게임 시작");
        startGameButton.setBackground(new Color(0, 150, 0));
        startGameButton.setForeground(Color.WHITE);

        // '방 나가기' 버튼
        JButton exitButton = new JButton("방 나가기");
        exitButton.setBackground(new Color(200, 50, 50));
        exitButton.setForeground(Color.WHITE);

        // 버튼들을 담을 패널 생성 (시작 버튼과 나가기 버튼)
        JPanel buttonGroup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonGroup.add(startGameButton); // ★ 시작 버튼 추가
        buttonGroup.add(exitButton);

        bottomPanel.add(chatField, BorderLayout.CENTER);
        bottomPanel.add(buttonGroup, BorderLayout.EAST); // 버튼 그룹을 EAST에 배치

        add(bottomPanel, BorderLayout.SOUTH);

        // --- 이벤트 리스너 ---
        chatField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String msg = chatField.getText();
                if (!msg.isEmpty()) {
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
                }
            }
        });

        // ★ 게임 시작 버튼 리스너
        startGameButton.addActionListener(e -> {
            try {
                // 서버에 /start 명령 전송
                if (mainFrame.getSocket() != null) {
                    PrintWriter out = new PrintWriter(mainFrame.getSocket().getOutputStream(), true);
                    out.println(Protocol.CMD_START);
                    System.out.println("[Client] 게임 시작 요청 전송");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
                appendMessage("[System] 서버 통신 오류: 게임 시작 요청 실패");
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

    // 대기방 초기화
    public void reset() {
        chatArea.setText(""); // 이전 채팅 기록 삭제
        chatField.setText("");
        // 유저 목록은 서버에서 /userlist를 다시 보내주므로 비워도 됨
        userListModel.clear();
    }
}
