package client;

import common.Protocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.PrintWriter;

public class RoomListPanel extends JPanel {
    private MainFrame mainFrame;
    private JList<String> roomList;
    private DefaultListModel<String> roomListModel;

    public RoomListPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(Color.white);

        // 상단 타이틀
        JLabel titleLabel = new JLabel("참여할 방을 선택하세요");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 24));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(titleLabel, BorderLayout.NORTH);

        // 중앙: room list
        roomListModel = new DefaultListModel<>();
        roomList = new JList<>(roomListModel);
        roomList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        roomList.setFont(new Font("맑은 고딕", Font.PLAIN, 16));

        // 더블 클릭 시 바로 입장
        roomList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    requestJoin();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(roomList);
        scrollPane.setBorder(BorderFactory.createTitledBorder("개설된 방 목록"));
        add(scrollPane, BorderLayout.CENTER);

        // 하단: 버튼들
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        bottomPanel.setBackground(Color.white);

        JButton refreshButton = new JButton("새로고침");
        JButton joinButton = new JButton("입장하기");
        JButton backButton = new JButton("뒤로가기");

        // 버튼 스타일
        Dimension btnSize = new Dimension(100, 40);
        refreshButton.setPreferredSize(btnSize);
        joinButton.setPreferredSize(btnSize);
        backButton.setPreferredSize(btnSize);
        joinButton.setBackground(new Color(200, 230, 255));

        bottomPanel.add(refreshButton);
        bottomPanel.add(joinButton);
        bottomPanel.add(backButton);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- 이벤트 리스너 ---
        refreshButton.addActionListener(e -> requestRoomList());    // 서버에 방 목록 요청
        joinButton.addActionListener(e -> requestJoin());           // 선택된 방으로 입장 요청
        backButton.addActionListener(e -> mainFrame.changePanel(MainFrame.LOBBY_PANEL));    // 메인 로비로
    }

    // 서버에 방 목록 요청
    public void requestRoomList() {
        try {
            PrintWriter out = new PrintWriter(mainFrame.getSocket().getOutputStream(), true);
            out.println(Protocol.CMD_ROOMLIST);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // 서버에 입장 요청
    private void requestJoin() {
        String selected = roomList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "방을 선택해주세요.");
            return;
        }

        String roomName = selected.trim();

        try {
            PrintWriter out = new PrintWriter(mainFrame.getSocket().getOutputStream(), true);
            out.println(Protocol.CMD_JOIN + " " + roomName);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // MainFrame이 호출해둘 메소드(ui 갱신)
    public void updateRoomList(String[] rooms) {
        roomListModel.clear();
        for (String room : rooms) {
            if(!room.isEmpty()) {
                roomListModel.addElement(room);
            }
        }
    }
}
