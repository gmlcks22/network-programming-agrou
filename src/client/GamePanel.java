package client;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GamePanel extends JPanel {

    // 1. 화면 전환용
    private MainFrame mainPanel;

    // 2. UI 컴포넌트
    private JLabel populationLabel; // 인원수 표시 레이블
    private int currentPopulation = 4; // 현재 인원수
    
    // (중요) 오른쪽에 "선택한 역할"을 표시할 패널
    private JPanel selectedRolesListPanel;

    public GamePanel(MainFrame mainPanel) {
        this.mainPanel = mainPanel;

        this.setBackground(Color.WHITE);
        this.setLayout(new BorderLayout(20, 20)); // 전체 레이아웃
        // 화면 전체 여백
        this.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // 3. (NORTH) - 전체 타이틀
        JLabel titleLabel = new JLabel("게임 생성하기");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 30));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(titleLabel, BorderLayout.NORTH);

        // 4. (CENTER) - 메인 컨텐츠 (왼쪽, 중앙, 오른쪽)
        JPanel mainContentPanel = new JPanel(new BorderLayout(30, 20)); // 컴포넌트 사이 간격
        mainContentPanel.setBackground(Color.WHITE);
        this.add(mainContentPanel, BorderLayout.CENTER);

        // --- 4-1. (WEST) 왼쪽 정보 패널 (방번호, 인원수) ---
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS)); // Y축으로 쌓기
        leftPanel.setBackground(Color.WHITE);

        // 방 번호
        JPanel roomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        roomPanel.setBackground(Color.WHITE);
        JLabel roomLabel = new JLabel("방 번호");
        roomLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        JTextField roomField = new JTextField("10394813"); // 이미지의 텍스트
        roomField.setEditable(false);
        roomField.setBackground(new Color(240, 240, 240));
        roomField.setPreferredSize(new Dimension(100, 30));
        
        roomPanel.add(roomLabel);
        roomPanel.add(Box.createRigidArea(new Dimension(10, 0))); // 간격
        roomPanel.add(roomField);
        
        // 인원수
        JPanel popPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        popPanel.setBackground(Color.WHITE);
        JLabel popLabelTitle = new JLabel("인원수");
        popLabelTitle.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        JButton minusButton = new JButton("-");
        JButton plusButton = new JButton("+");
        populationLabel = new JLabel(currentPopulation + "명"); // "4명"
        populationLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        Dimension buttonSize = new Dimension(45, 30); // 버튼 크기
        minusButton.setPreferredSize(buttonSize);
        plusButton.setPreferredSize(buttonSize);
        
        popPanel.add(popLabelTitle);
        popPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        popPanel.add(minusButton);
        popPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        popPanel.add(populationLabel);
        popPanel.add(Box.createRigidArea(new Dimension(5, 0)));
        popPanel.add(plusButton);
        
        // 왼쪽 패널에 추가
        leftPanel.add(roomPanel);
        leftPanel.add(popPanel);
        leftPanel.add(Box.createVerticalGlue()); // 컴포넌트들을 위로 정렬

        mainContentPanel.add(leftPanel, BorderLayout.WEST);

        // --- 4-2. (CENTER) 중앙 역할 선택 그리드 ---
        // "역할 선택" 부제 타이틀이 있는 보더 추가
        JPanel roleGridWrapper = new JPanel(new BorderLayout());
        roleGridWrapper.setBackground(Color.WHITE);
        roleGridWrapper.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "역할 선택",
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 16)
        ));

        // 요청하신 5개 역할 (임시 버튼으로)
        // (나중에는 이미지를 넣은 JButton 등으로 교체)
        JPanel roleGridPanel = new JPanel(new GridLayout(2, 3, 10, 10)); // 2행 3열
        roleGridPanel.setBackground(Color.WHITE);
        roleGridPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 내부 여백

        String[] roleNames = {"마피아", "시민", "경찰", "의사", "기타 역할"};
        
        for (String roleName : roleNames) {
            JButton roleButton = new JButton(roleName); // (임시) 텍스트 버튼
            roleButton.setBackground(new Color(220, 220, 220));
            roleButton.setPreferredSize(new Dimension(100, 100)); // 정사각형
            roleButton.setFont(new Font("SansSerif", Font.BOLD, 14));
            
            // (기능) 역할 버튼 클릭 리스너
            roleButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.out.println(roleName + " 역할 클릭");
                    // (임시) 클릭하면 오른쪽에 추가 (중복 체크 X)
                    addRoleToSelectedList(roleName);
                }
            });
            roleGridPanel.add(roleButton);
        }
        
        roleGridWrapper.add(roleGridPanel, BorderLayout.CENTER);
        mainContentPanel.add(roleGridWrapper, BorderLayout.CENTER);

        // --- 4-3. (EAST) 오른쪽 선택한 역할 패널 ---
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setBackground(Color.WHITE);
        rightPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "선택한 역할",
                TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 16)
        ));
        
        // 실제 역할 목록이 들어갈 패널
        selectedRolesListPanel = new JPanel();
        selectedRolesListPanel.setLayout(new BoxLayout(selectedRolesListPanel, BoxLayout.Y_AXIS));
        selectedRolesListPanel.setBackground(Color.WHITE);
        selectedRolesListPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // 내부 여백
        
        rightPanel.add(selectedRolesListPanel, BorderLayout.NORTH); // 북쪽에 붙여서 쌓음
        rightPanel.setPreferredSize(new Dimension(150, 0)); // 너비 고정
        mainContentPanel.add(rightPanel, BorderLayout.EAST);

        // 5. (SOUTH) - 하단 확인 버튼
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bottomPanel.setBackground(Color.WHITE);
        
        JButton confirmButton = new JButton("확인");
        confirmButton.setBackground(new Color(220, 220, 220));
        confirmButton.setPreferredSize(new Dimension(100, 40));
        confirmButton.setFont(new Font("SansSerif", Font.BOLD, 14));
        
        bottomPanel.add(confirmButton);
        this.add(bottomPanel, BorderLayout.SOUTH);

        // --- 6. (기능) 버튼 액션 리스너 구현 ---
        
        // 인원수 감소
        minusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPopulation > 4) { // (임시) 최소 4명
                    currentPopulation--;
                    populationLabel.setText(currentPopulation + "명");
                }
            }
        });

        // 인원수 증가
        plusButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentPopulation < 10) { // (임시) 최대 10명
                    currentPopulation++;
                    populationLabel.setText(currentPopulation + "명");
                }
            }
        });

        // 확인 버튼 (임시 - 로비로 돌아가기)
        confirmButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("확인 버튼 클릭 (게임 생성 로직 필요)");
                // (나중에) 실제 게임 생성 로직 + 서버에 방 생성 요청
                
                // (임시) 로비로 돌아가기
                cardLayout.show(mainPanel, MainFrame.LOBBY_PANEL);
            }
        });
    }

    /**
     * (헬퍼 메소드) "선택한 역할" 목록에 역할을 동적으로 추가
     */
    private void addRoleToSelectedList(String roleName) {
        JLabel roleLabel = new JLabel(roleName);
        roleLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        selectedRolesListPanel.add(roleLabel);
        selectedRolesListPanel.add(Box.createRigidArea(new Dimension(0, 10))); // 역할 사이 간격
        
        // (중요) 패널에 UI 컴포넌트가 추가/삭제되면 revalidate/repaint 호출
        selectedRolesListPanel.revalidate();
        selectedRolesListPanel.repaint();
    }
}