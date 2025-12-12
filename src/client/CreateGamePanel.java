package client;

import common.Protocol;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class CreateGamePanel extends JPanel {

    // 1. 화면 전환용
    private MainFrame mainFrame;

    // 2. UI 컴포넌트
    private JLabel populationLabel; // 인원수 표시 레이블
    private int currentPopulation = 4; // 현재 인원수
    private JPanel selectedRolesListPanel; // 역할이 쌓일 패널
    private JTextField roomField;

    // 역할별 진영 정보
    private static final Map<String, String> ROLE_FACTIONS = Map.of(
            "경비병", "Citizen",
            "늑대인간", "Mafia",
            "독재자", "Citizen",
            "사냥꾼", "Citizen",
            "선견자", "Citizen",
            "시민", "Citizen",
            "천사", "Citizen"
    );

    private int selectedRoleCount = 0;
    private int selectedMafiaCount = 0; // 마피아 진영 수 추적용 필드

    // 선택된 역할 정보를 담고 취소 기능을 내장
    private class SelectedRoleLabel extends JLabel {

        private final String roleName;
        private final String faction;

        public SelectedRoleLabel(String roleName, String faction) {
            super(roleName);
            this.roleName = roleName;
            this.faction = faction;
            setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float)14));
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));

            setToolTipText("클릭하여 목록에서 제거");
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    removeRoleFromSelectedList(SelectedRoleLabel.this);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    setBackground(new Color(220, 50, 50));
                    setForeground(Color.WHITE); // text color
                    // 취소선 효과 적용
                    setText("<html><strike>" + roleName + "</strike></html>");
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    setBackground(UIManager.getColor("Panel.background"));
                    setForeground(UIManager.getColor("Panel.foreground"));
                    setText(roleName);
                }
            });
        }

        public String getRoleFaction() {
            return faction;
        }
    }

    public CreateGamePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        this.setLayout(new BorderLayout(20, 20)); // 전체 레이아웃
        this.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // 3. (NORTH) - 전체 타이틀
        JLabel titleLabel = new JLabel("게임 생성하기");
        titleLabel.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float)30));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        this.add(titleLabel, BorderLayout.NORTH);

        // 메인 컨텐츠 패널 (좌, 중, 우 포함)
        JPanel mainContentPanel = new JPanel(new BorderLayout(20, 20));
        this.add(mainContentPanel, BorderLayout.CENTER);

        //  왼쪽 설정 패널 (방번호, 인원수) ---
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));

        // 방 번호
        JPanel roomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel roomLabel = new JLabel("방 번호");
        roomLabel.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float)14));

        roomField = new JTextField("10394813");
        roomField.setEditable(false);
        roomField.setPreferredSize(new Dimension(100, 30));

        roomPanel.add(roomLabel);
        roomPanel.add(Box.createRigidArea(new Dimension(10, 0)));
        roomPanel.add(roomField);

        // 인원수 조절
        JPanel popPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel popLabelTitle = new JLabel("인원수");
        popLabelTitle.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float)14));

        JButton minusButton = new JButton("-");
        JButton plusButton = new JButton("+");
        populationLabel = new JLabel(currentPopulation + "명");
        populationLabel.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float)14));

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
        roleGridWrapper.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "역할 선택",
                TitledBorder.CENTER, TitledBorder.TOP,
                UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float)16)
        ));

        // 5열 그리드
        JPanel roleGridPanel = new JPanel(new GridLayout(0, 5, 5, 5));
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
                Image newImg = img.getScaledInstance(50, 75, Image.SCALE_SMOOTH);
                roleButton = new JButton(new ImageIcon(newImg));
            } else {
                roleButton = new JButton(roleName);
            }

            roleButton.setPreferredSize(new Dimension(65, 95));
            roleButton.setMargin(new Insets(0, 0, 0, 0));
            roleButton.setBorderPainted(false);
            roleButton.setMargin(new Insets(0, 0, 0, 0));
            roleButton.setBorder(null);
            roleButton.setContentAreaFilled(false);
            roleButton.setFocusPainted(false);

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
        rightPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "선택한 역할",
                TitledBorder.LEFT, TitledBorder.TOP,
                UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float)16)
        ));

        // 역할 목록이 들어갈 내부 패널
        selectedRolesListPanel = new JPanel();
        selectedRolesListPanel.setLayout(new BoxLayout(selectedRolesListPanel, BoxLayout.Y_AXIS));
        selectedRolesListPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JScrollPane scrollPane = new JScrollPane(selectedRolesListPanel);
        scrollPane.setBorder(null);

        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        rightPanel.add(scrollPane, BorderLayout.CENTER);
        rightPanel.setPreferredSize(new Dimension(160, 0));
        mainContentPanel.add(rightPanel, BorderLayout.EAST);

        // 하단 버튼
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        // 취소 버튼
        JButton cancelButton = new JButton("취소");
        cancelButton.setBackground(new Color(80, 80, 80));
        cancelButton.setForeground(Color.WHITE);
        cancelButton.setPreferredSize(new Dimension(100, 40));
        cancelButton.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float)14));
        //확인 버튼
        JButton confirmButton = new JButton("확인");
        confirmButton.setBackground(new Color(180, 0, 0));
        confirmButton.setForeground(Color.WHITE);
        confirmButton.setPreferredSize(new Dimension(100, 40));
        confirmButton.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float)14));

        bottomPanel.add(cancelButton);
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
        cancelButton.addActionListener(e -> {
            resetData(); // 입력했던 데이터 초기화
            mainFrame.changePanel(MainFrame.LOBBY_PANEL); // 로비로 이동
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

            // 선택된 역할 목록을 문자열
            StringBuilder roleListBuilder = new StringBuilder();
            Component[] components = selectedRolesListPanel.getComponents();
            for (Component comp : components) {
                if (comp instanceof SelectedRoleLabel) {
                    SelectedRoleLabel roleLabel = (SelectedRoleLabel) comp;
                    roleListBuilder.append(roleLabel.roleName).append(",");
                }
            }
            // 마지막 쉼표 제거 (예: Wolf,Guard,Citizen,) -> Wolf,Guard,Citizen
            String roleListStr = roleListBuilder.length() > 0 ? roleListBuilder.substring(0, roleListBuilder.length() - 1) : "";

            // 인원수가 일치하면 서버 통신 진행
            String randomRoomNum = roomField.getText();
            String roomTitle = randomRoomNum;

            try {
                if (mainFrame.getSocket() == null) {
                    JOptionPane.showMessageDialog(this, "서버에 연결되지 않았습니다.");
                    return;
                }

                PrintWriter out = new PrintWriter(mainFrame.getSocket().getOutputStream(), true);
                // CMD_CREATE 포맷: 방제목과 역할 목록을 함께 전송
                out.println(Protocol.CMD_CREATE + " " + roomTitle + " " + roleListStr);
                System.out.println("[Client] 방 생성 요청: " + roomTitle + ", 역할: " + roleListStr);

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
                reset(); // 패널이 보일 때 리셋
            }
        });
    }

    // 화면 데이터를 처음 상태로 리셋하는 메소드
    public void reset() {
        // 인원수 리셋
        currentPopulation = 4;
        populationLabel.setText(currentPopulation + "명");

        // 역할 수 초기화
        selectedRoleCount = 0;
        selectedMafiaCount = 0;

        // 방 번호 새로 랜덤 생성
        int randomNum = (int) (Math.random() * 10000000) + 1000000;
        roomField.setText(String.valueOf(randomNum));

        // 선택된 역할 목록 비우기
        selectedRolesListPanel.removeAll();
        selectedRolesListPanel.revalidate();
        selectedRolesListPanel.repaint();

        System.out.println("[Client] 게임 생성 화면 데이터 리셋 완료");
    }

    // 선택된 역할 목록에서 특정 역할을 제거하고 카운터를 업데이트
    private void removeRoleFromSelectedList(SelectedRoleLabel roleLabel) {

        String roleName = roleLabel.getText();
        String faction = roleLabel.getRoleFaction();

        // 1. 카운터 감소
        selectedRoleCount--;

        if (faction.equals("Mafia")) {
            selectedMafiaCount--;
        }

        // 제거된 역할 레이블 뒤에 있는 RigidArea (간격)도 찾아서 제거해야 깔끔합니다.
        Component[] components = selectedRolesListPanel.getComponents();
        for (int i = 0; i < components.length; i++) {
            // 현재 제거하려는 레이블과 일치하는 컴포넌트를 찾음
            if (components[i] == roleLabel) {
                // 역할 레이블 바로 다음에 있는 간격(RigidArea) 제거
                if (i + 1 < components.length && components[i + 1] instanceof Box.Filler) {
                    selectedRolesListPanel.remove(components[i + 1]);
                }
                break; // 역할 레이블과 간격을 제거했으므로 반복문 종료
            }
        }

        // 2. UI에서 제거
        selectedRolesListPanel.remove(roleLabel);

        System.out.println("역할 제거: " + roleName + ", 남은 역할 수: " + selectedRoleCount + ", 남은 늑대 수: " + selectedMafiaCount);

        selectedRolesListPanel.revalidate();
        selectedRolesListPanel.repaint();
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

        String faction = ROLE_FACTIONS.getOrDefault(roleName, "Citizen");

        // 마피아 진영 제한 조건 체크 및 카운터 증가
        if (faction.equals("Mafia")) {
            // 마피아 진영 제한 조건: 인원수의 1/3
            final int MAX_WOLF = currentPopulation / 3;

            if (selectedMafiaCount >= MAX_WOLF) {
                JOptionPane.showMessageDialog(this,
                        currentPopulation + "인 방에서는 마피아 진영을 최대 " + MAX_WOLF + "명까지만 선택할 수 있습니다.",
                        "밸런스 오류",
                        JOptionPane.WARNING_MESSAGE);
                return; // 마피아 수 제한에 걸리면 추가하지 않고 종료
            }
            selectedMafiaCount++; // 마피아 수 증가
        }

        // 역할 추가 로직: SelectedRoleLabel을 사용하여 제거 기능 활성화
        SelectedRoleLabel roleLabel = new SelectedRoleLabel(roleName, faction);

        selectedRolesListPanel.add(roleLabel);
        selectedRolesListPanel.add(Box.createRigidArea(new Dimension(0, 8)));

        // 역할 수 증가
        selectedRoleCount++;
        System.out.println("역할 추가: " + roleName + ", 현재 늑대 수: " + selectedMafiaCount);

        selectedRolesListPanel.revalidate();
        selectedRolesListPanel.repaint();
    }
}
