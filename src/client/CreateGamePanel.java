package client;

import common.Protocol;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class CreateGamePanel extends JPanel {

    // 1. 화면 전환용
    private MainFrame mainFrame;

    // 2. UI 컴포넌트
    private JLabel populationLabel; // 인원수 표시 레이블
    private int currentPopulation = 4; // 현재 인원수
    private JPanel selectedRolesListPanel; // 역할이 쌓일 패널
    private JTextField roomField; 
    
    // 현재 선택된 역할의 총 인원수
    private int selectedRoleCount = 0; 

    public CreateGamePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        this.setBackground(Color.WHITE);
        this.setLayout(new BorderLayout(20, 20)); // 전체 레이아웃
        this.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // 3. (NORTH) - 전체 타이틀
        JLabel titleLabel = new JLabel("게임 생성하기");
        titleLabel.setFont(new Font("맑은 고딕", Font.BOLD, 30));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(titleLabel, BorderLayout.NORTH);

        // 메인 컨텐츠 패널 (좌, 중, 우 포함)
        JPanel mainContentPanel = new JPanel(new BorderLayout(20, 20));
        mainContentPanel.setBackground(Color.WHITE);
        this.add(mainContentPanel, BorderLayout.CENTER);


        //  왼쪽 설정 패널 (방번호, 인원수) ---
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setBackground(Color.WHITE);

        // 방 번호
        JPanel roomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        roomPanel.setBackground(Color.WHITE);
        JLabel roomLabel = new JLabel("방 번호");
        roomLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        
        roomField = new JTextField("10394813");
        roomField.setEditable(false);
        roomField.setBackground(new Color(240, 240, 240));
        roomField.setPreferredSize(new Dimension(100, 30));
        
        roomPanel.add(roomLabel);
        roomPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        roomPanel.add(roomField);

        // 인원수 조절
        JPanel popPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        popPanel.setBackground(Color.WHITE);
        JLabel popLabelTitle = new JLabel("인원수");
        popLabelTitle.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        
        JButton minusButton = new JButton("-");
        JButton plusButton = new JButton("+");
        populationLabel = new JLabel(currentPopulation + "명");
        populationLabel.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        
        Dimension buttonSize = new Dimension(45, 30);
        minusButton.setPreferredSize(buttonSize);
        plusButton.setPreferredSize(buttonSize);
        
        popPanel.add(popLabelTitle);
        popPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        popPanel.add(minusButton);
        popPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        popPanel.add(populationLabel);
        popPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        popPanel.add(plusButton);

        // 왼쪽 패널에 조립
        leftPanel.add(roomPanel);
        leftPanel.add(popPanel);
        leftPanel.add(Box.createVerticalGlue());
        mainContentPanel.add(leftPanel, BorderLayout.WEST);


        // 중앙 역할 선택 그리드 ---
        JPanel roleGridWrapper = new JPanel(new BorderLayout());
        roleGridWrapper.setBackground(Color.WHITE);
        roleGridWrapper.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "역할 선택",
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("맑은 고딕", Font.BOLD, 16)
        ));

        // 5열 그리드
        JPanel roleGridPanel = new JPanel(new GridLayout(0, 5, 5, 5));
        roleGridPanel.setBackground(Color.WHITE);
        roleGridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 역할 데이터
        String[] roleNames = {"경비병", "늑대인간", "독재자", "마녀", "사냥꾼", "선견자", "시민", "천사", "큐피드"};
        String[] roleImages = {"경비병.png", "늑대인간.png", "독재자.png", "마녀.png", "사냥꾼.png", "선견자.png", "시민.png", "천사.png", "큐피드.png"};

        for (int i = 0; i < roleNames.length; i++) {
            String roleName = roleNames[i];
            String fileName = roleImages[i];

            String path1 = "src/resources/images/" + fileName;

            File imgFile = new File(path1);

            JButton roleButton;

            if (imgFile.exists()) {
                ImageIcon originalIcon = new ImageIcon(imgFile.getAbsolutePath());
                Image img = originalIcon.getImage();
                Image newImg = img.getScaledInstance(55, 85, Image.SCALE_SMOOTH);
                roleButton = new JButton(new ImageIcon(newImg));
            } else {
                roleButton = new JButton(roleName);
            }

            roleButton.setPreferredSize(new Dimension(65, 95));
            roleButton.setBackground(Color.WHITE);
            roleButton.setMargin(new Insets(0, 0, 0, 0));
            roleButton.setBorderPainted(false);

            roleButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    addRoleToSelectedList(roleName);
                }
            });
            roleGridPanel.add(roleButton);
        }

        roleGridWrapper.add(roleGridPanel, BorderLayout.CENTER);
        mainContentPanel.add(roleGridWrapper, BorderLayout.CENTER);


        // 오른쪽 선택한 역할 패널---
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "선택한 역할",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("맑은 고딕", Font.BOLD, 16)
        ));

        // 역할 목록이 들어갈 내부 패널
        selectedRolesListPanel = new JPanel();
        selectedRolesListPanel.setLayout(new BoxLayout(selectedRolesListPanel, BoxLayout.Y_AXIS));
        selectedRolesListPanel.setBackground(Color.WHITE);
        selectedRolesListPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(selectedRolesListPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);

        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        rightPanel.add(scrollPane, BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(160, 0));
        mainContentPanel.add(rightPanel, BorderLayout.EAST);


        // 하단 버튼
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(Color.WHITE);

        JButton confirmButton = new JButton("확인");
        confirmButton.setBackground(new Color(220, 220, 220));
        confirmButton.setPreferredSize(new Dimension(100, 40));
        confirmButton.setFont(new Font("맑은 고딕", Font.BOLD, 14));
        
        bottomPanel.add(confirmButton);
        this.add(bottomPanel, BorderLayout.SOUTH);


        // --- 이벤트 리스너 설정 ---

        minusButton.addActionListener(e -> {
            if (currentPopulation > 4) {
                currentPopulation--;
                populationLabel.setText(currentPopulation + "명");
            }
        });

        plusButton.addActionListener(e -> {
            if (currentPopulation < 10) {
                currentPopulation++;
                populationLabel.setText(currentPopulation + "명");
            }
        });

        // 확인 버튼 -> 서버 통신 및 화면 전환 로직
        confirmButton.addActionListener(e -> {
            
            // 선택된 역할의 총합과 인원수가 동일한지 검사
            if (selectedRoleCount != currentPopulation) {
                JOptionPane.showMessageDialog(this, 
                    "선택된 역할의 총합(" + selectedRoleCount + "명)이 인원수(" + currentPopulation + "명)와 일치해야 합니다.", 
                    "인원 불일치", 
                    JOptionPane.WARNING_MESSAGE);
                return; // 일치하지 않으면 여기서 중단
            }

            // 인원수가 일치하면 서버 통신 진행
            String randomRoomNum = roomField.getText();
            String roomTitle = randomRoomNum;

            try {
                if (mainFrame.getSocket() == null) {
                    JOptionPane.showMessageDialog(this, "서버에 연결되지 않았습니다.");
                    return;
                }

                PrintWriter out = new PrintWriter(mainFrame.getSocket().getOutputStream(), true);
                out.println(Protocol.CMD_CREATE + " " + roomTitle);
                System.out.println("[Client] 방 생성 요청: " + roomTitle);

                mainFrame.changePanel(MainFrame.WAITING_PANEL);

            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "통신 오류 발생!");
            }
        });

        // 이 패널이 화면에 보일 때마다 자동으로 초기화 실행
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                resetData();
            }
        });
    }

    // 화면 데이터를 처음 상태로 리셋하는 메소드
    private void resetData() {
        // 인원수 리셋
        currentPopulation = 4;
        populationLabel.setText(currentPopulation + "명");
        
        // 역할 수 초기화
        selectedRoleCount = 0; 

        // 방 번호 새로 랜덤 생성
        int randomNum = (int)(Math.random() * 10000000) + 1000000; 
        roomField.setText(String.valueOf(randomNum));

        // 선택된 역할 목록 비우기
        selectedRolesListPanel.removeAll();
        selectedRolesListPanel.revalidate();
        selectedRolesListPanel.repaint();

        System.out.println("[Client] 게임 생성 화면 데이터 리셋 완료");
    }

    // 오른쪽 패널에 역할 텍스트 추가하는 함수
    private void addRoleToSelectedList(String roleName) {
        
        // 총 인원수를 초과하는지 검사
        if (selectedRoleCount >= currentPopulation) {
            JOptionPane.showMessageDialog(this, 
                "총 인원수(" + currentPopulation + "명)를 초과할 수 없습니다.", 
                "인원 초과", 
                JOptionPane.WARNING_MESSAGE);
            return; // 초과하면 추가하지 않고 종료
        }

        // 역할 추가 로직
        JLabel roleLabel = new JLabel(roleName);
        roleLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 14));
        roleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        selectedRolesListPanel.add(roleLabel);
        selectedRolesListPanel.add(Box.createRigidArea(new Dimension(0, 8))); 
        
        // 역할 수 증가
        selectedRoleCount++;
        System.out.println("역할 추가: " + roleName + ", 현재 총 역할 수: " + selectedRoleCount);

        selectedRolesListPanel.revalidate();
        selectedRolesListPanel.repaint();
    }
}