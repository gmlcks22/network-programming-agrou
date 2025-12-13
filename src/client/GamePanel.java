package client;

import common.Protocol;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.*;
import javax.swing.border.TitledBorder;

public class GamePanel extends JPanel {

    private MainFrame mainFrame;

    // --- 상단 (Top) UI ---
    private JPanel roleBookPanel;   // 직업 도감 (상단 이동)
    private JLabel phaseLabel;
    private JProgressBar timerProgressBar;
    private JLabel survivorCountLabel;

    // --- 중앙 (Center) UI ---
    private JPanel centerInfoPanel; // 안내 문구 표시

    // --- 하단 (Bottom) UI ---
    private JTextArea chatArea;
    private JTextField chatField;
    private JButton chatSendButton;
    private JComboBox<String> chatModeCombo;
    private JScrollPane chatScrollPane;

    private JPanel playerGridPanel; // 플레이어 선택 패널 (하단 중앙 이동)

    // --- 데이터 및 상태 관리 ---
    private String myRoleName = "시민";
    private String myFaction = "Citizen";
    private int survivorCount = 0;

    private String currentPhase = "WAITING";
    private Timer clientTimer;
    private int remainingSeconds = 0;
    private int maxSeconds = 1; // Progress bar 계산용 전체 시간

    // 플레이어 버튼 관리 (닉네임 -> 버튼)
    private Map<String, JButton> playerButtons = new HashMap<>();
    // 직업 도감 라벨 관리 (직업명 -> 라벨, 테두리 표시용)
    private Map<String, JLabel> roleBookLabels = new HashMap<>();

    // 사망자 관리 및 내 상태
    private Set<String> deadPlayers = new HashSet<>();
    private boolean amIDead = false;

    // 큐피드
    private boolean isLover = false;
    private Set<String> cupidTargets = new HashSet<>();

    // 사냥꾼 발포 모드 여부
    private boolean isHunterMode = false;

    // 직업 설명 데이터
    private static final Map<String, String> ROLE_DESCRIPTIONS = new HashMap<>();
    static {
        ROLE_DESCRIPTIONS.put("시민", "아무런 능력이 없습니다. 낮 동안의 토론과 투표를 통해 마피아를 찾아내야 합니다.");
        ROLE_DESCRIPTIONS.put("늑대인간", "마피아 진영입니다. 매일 밤 동료들과 상의하여 한 명의 시민을 살해할 수 있습니다.");
        ROLE_DESCRIPTIONS.put("경비병", "매일 밤 자신을 포함한 한 명을 선택하여 늑대인간의 공격으로부터 보호합니다.");
        ROLE_DESCRIPTIONS.put("선견자", "매일 밤 한 명을 선택하여 그 사람의 직업을 알아낼 수 있습니다.");
        ROLE_DESCRIPTIONS.put("마녀", "당신은 일회용 물약 2가지, 치료 물약과 독 물약을 사용할 수 있습니다.");
        ROLE_DESCRIPTIONS.put("사냥꾼", "자신이 사망할 때, 유언으로 다른 한 명을 지목하여 함께 데려갈 수 있습니다.");
        ROLE_DESCRIPTIONS.put("독재자", "투표 시간에 능력을 사용하여 혼자서 처형 대상을 결정할 수 있습니다. (1회)");
        ROLE_DESCRIPTIONS.put("천사", "첫날 낮 투표에서 처형당하면 즉시 게임에서 승리합니다.");
        ROLE_DESCRIPTIONS.put("큐피드", "첫날 밤 두 명을 연인으로 지정합니다. 한 명이 죽으면 다른 한 명도 함께 죽습니다.");
    }

    public GamePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());

        initTopPanel();
        initCenterPanel();
        initBottomPanel();
    }

    // 1. 상단 패널: 직업도감(좌) + 타이머/단계(중) + 생존자(우)
    private void initTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        topPanel.setPreferredSize(new Dimension(0, 85));

        // [좌측] 직업 도감
        roleBookPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        roleBookPanel.setOpaque(false);

        JScrollPane roleScrollPane = new JScrollPane(roleBookPanel);
        roleScrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
        roleScrollPane.getViewport().setOpaque(false);
        roleScrollPane.setOpaque(false);
        roleScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        roleScrollPane.setPreferredSize(new Dimension(250, 85)); // 너비 제한

        topPanel.add(roleScrollPane, BorderLayout.WEST);

        // [중앙] 타이머 및 단계
        JPanel statusPanel = new JPanel(new GridLayout(2, 1));
        statusPanel.setOpaque(false);

        phaseLabel = new JLabel("게임 대기 중");
        phaseLabel.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float) 16));
        phaseLabel.setHorizontalAlignment(SwingConstants.CENTER);

        timerProgressBar = new JProgressBar();
        timerProgressBar.setValue(0);
        timerProgressBar.setStringPainted(true); // 바 안에 N초 텍스트 표시
        timerProgressBar.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, 12f));
        timerProgressBar.setForeground(new Color(100, 200, 100)); // 초기 초록색
        timerProgressBar.setBackground(Color.decode("#323236"));
        timerProgressBar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI() {
            protected Color getSelectionBackground() { return Color.WHITE; } // 바가 없는 곳의 글자 색
            protected Color getSelectionForeground() { return Color.WHITE; } // 바가 채워진 곳의 글자 색
        });
        timerProgressBar.setPreferredSize(new Dimension(200, 8)); // 바 크기 설정(statusPanel이라 자동으로 높이 지정됨)

        statusPanel.add(phaseLabel, BorderLayout.NORTH);
        statusPanel.add(timerProgressBar, BorderLayout.CENTER);
        topPanel.add(statusPanel, BorderLayout.CENTER);

        // [우측] 생존자 수
        survivorCountLabel = new JLabel("생존자: - 명");
        survivorCountLabel.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float)16));

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightPanel.setOpaque(false);
        rightPanel.add(survivorCountLabel);
        rightPanel.setPreferredSize(new Dimension(250, 85));
        topPanel.add(rightPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
    }

    // 2. 중앙 패널: 단순 정보 표시 (카드 레이아웃 제거)
    private void initCenterPanel() {
        centerInfoPanel = new JPanel(new BorderLayout());
        centerInfoPanel.setOpaque(false);
        centerInfoPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel infoLabel = new JLabel("<html><div style='text-align: center; color: gray;'>" +
                "<h1>Wolf Mafia</h1>" +
                "<p>상단 왼쪽의 도감을 클릭하여 직업 설명을 확인하세요.<br>" +
                "하단에서 대상을 선택하여 투표하거나 능력을 사용하세요.</p></div></html>");
        infoLabel.setHorizontalAlignment(SwingConstants.CENTER);

        centerInfoPanel.add(infoLabel, BorderLayout.CENTER);
        add(centerInfoPanel, BorderLayout.CENTER);
    }

    // 3. 하단 패널: 채팅(좌) + 플레이어 선택(중)
    private void initBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setPreferredSize(new Dimension(0, 300)); // 높이 300px 고정

        // 3-1. [좌측] 채팅창 (기존 로직 유지)
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("채팅"));
        chatPanel.setPreferredSize(new Dimension(300, 0));

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatScrollPane = new JScrollPane(chatArea);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());

        chatModeCombo = new JComboBox<>();
        chatModeCombo.addItem("전체");
        chatModeCombo.setPreferredSize(new Dimension(70, 25));
        chatModeCombo.setVisible(false);

        chatField = new JTextField();
        chatField.addActionListener(e -> sendChatMessage());
        chatSendButton = new JButton("전송");
        chatSendButton.addActionListener(e -> sendChatMessage());

        JPanel leftInput = new JPanel(new BorderLayout());
        leftInput.add(chatModeCombo, BorderLayout.WEST);
        leftInput.add(chatField, BorderLayout.CENTER);

        inputPanel.add(leftInput, BorderLayout.CENTER);
        inputPanel.add(chatSendButton, BorderLayout.EAST);

        chatPanel.add(inputPanel, BorderLayout.SOUTH);
        bottomPanel.add(chatPanel, BorderLayout.WEST);

        // 3-2. [중앙] 플레이어 선택 패널 (높이 줄임 + 중앙 정렬)
        JPanel centerWrapper = new JPanel(new GridBagLayout());
        centerWrapper.setOpaque(false);
        centerWrapper.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        playerGridPanel = new JPanel(new GridLayout(0, 4, 10, 10)); // 4열 그리드

        TitledBorder gridBorder = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.BLUE, 2),
                "대상 선택",
                TitledBorder.CENTER, TitledBorder.TOP,
                UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float)14), Color.BLUE
        );
        playerGridPanel.setBorder(gridBorder);

        // GridBagConstraints: 수직으로 늘어나지 않게 설정
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0; // 높이 고정 (여백으로 채움)
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;

        centerWrapper.add(playerGridPanel, gbc);
        bottomPanel.add(centerWrapper, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // ================== [기능 구현] ==================

    // 1. 플레이어 사망 처리
    public void handlePlayerDeath(String deadNickname) {
        deadPlayers.add(deadNickname);

        if (survivorCount > 0) {
            survivorCount--;
            survivorCountLabel.setText("생존자: " + survivorCount + "명");
        }

        if (mainFrame.getNickname().equals(deadNickname)) {
            amIDead = true;
            appendMessage("[System] 당신은 사망하여 관전자 상태가 되었습니다.");
            setTargetSelectionEnabled(false);
            setChatEnabled(true);

            chatModeCombo.removeAllItems();
            chatModeCombo.addItem("유령");
            chatModeCombo.setSelectedItem("유령");
            chatModeCombo.setVisible(true);
            chatModeCombo.setEnabled(false);
        }

        refreshPlayerGrid();
    }

    // 2. 유저 목록 업데이트 (버튼 생성 - 자기 자신 제외)
    public void updateUserList(String[] users) {
        int currentSurvivors = 0;
        for (String u : users) {
            if (!deadPlayers.contains(u)) currentSurvivors++;
        }
        this.survivorCount = currentSurvivors;
        if (survivorCountLabel != null) {
            survivorCountLabel.setText("생존자: " + survivorCount + "명");
        }

        if (playerGridPanel != null) {
            playerGridPanel.removeAll();
            playerButtons.clear();

            for (String nickname : users) {
                if (nickname.isEmpty()) continue;
                // 자기 자신 버튼 생성 제외
                if (nickname.equals(mainFrame.getNickname())) continue;

                JButton playerBtn = new JButton(nickname);
                playerBtn.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float)14));
                playerBtn.setPreferredSize(new Dimension(80, 50));
                playerBtn.setFocusPainted(false);

                if (deadPlayers.contains(nickname)) {
                    playerBtn.setForeground(Color.DARK_GRAY);
                    playerBtn.setEnabled(false);
                    playerBtn.setText(nickname + " (사망)");
                } else {
                    playerBtn.setForeground(Color.BLACK);
                    playerBtn.setEnabled(true);
                }

                playerBtn.addActionListener(e -> handlePlayerClick(nickname));

                playerButtons.put(nickname, playerBtn);
                playerGridPanel.add(playerBtn);
            }
            playerGridPanel.revalidate();
            playerGridPanel.repaint();
        }
    }

    // 사망자 상태 갱신 (맵 이용)
    private void refreshPlayerGrid() {
        for (Map.Entry<String, JButton> entry : playerButtons.entrySet()) {
            String nickname = entry.getKey();
            JButton btn = entry.getValue();

            if (deadPlayers.contains(nickname)) {
                btn.setEnabled(false);
                btn.setForeground(Color.DARK_GRAY);
                if (!btn.getText().contains("(사망)")) {
                    btn.setText(nickname + " (사망)");
                }
            }
        }
        playerGridPanel.repaint();
    }

    // 3. 직업 도감 업데이트 (상단 표시, 팝업, 테두리 강조)
    public void updateRoleBook(String[] roles) {
        roleBookPanel.removeAll();
        roleBookLabels.clear();

        Set<String> uniqueRoles = new HashSet<>();
        for (String role : roles) uniqueRoles.add(role.trim());

        for (String roleName : uniqueRoles) {
            String imagePath = "src/resources/images/" + roleName + ".png";
            ImageIcon icon = loadScaledImage(imagePath, 40, 60);

            JLabel roleLabel = new JLabel();
            if (icon != null) {
                roleLabel.setIcon(icon);
            } else {
                roleLabel.setText(roleName);
                roleLabel.setPreferredSize(new Dimension(40, 60));
                roleLabel.setHorizontalAlignment(SwingConstants.CENTER);
            }

            roleLabel.setToolTipText(roleName);
            roleLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
            roleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            // 클릭 시 팝업
            roleLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    showRolePopup(roleName);
                }
            });

            roleBookLabels.put(roleName, roleLabel);
            roleBookPanel.add(roleLabel);
        }
        roleBookPanel.revalidate();
        roleBookPanel.repaint();

        highlightMyRole(); // 도감 갱신 후 내 직업 테두리 적용
    }

    // 내 직업 빨간 테두리 강조
    private void highlightMyRole() {
        for (JLabel lbl : roleBookLabels.values()) {
            lbl.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1));
        }
        JLabel myLabel = roleBookLabels.get(myRoleName);
        if (myLabel != null) {
            myLabel.setBorder(BorderFactory.createLineBorder(Color.RED, 3));
        }
    }

    // 직업 설명 팝업
    private void showRolePopup(String roleName) {
        String description = ROLE_DESCRIPTIONS.getOrDefault(roleName, "설명이 없습니다.");
        JOptionPane.showMessageDialog(this,
                "<html><body style='width: 200px;'><h2>" + roleName + "</h2><p>" + description + "</p></body></html>",
                "직업 설명", JOptionPane.INFORMATION_MESSAGE);
    }

    // 4. 내 직업 설정
    public void setMyRole(String roleName, String faction) {
        this.myRoleName = roleName;
        this.myFaction = faction;

        highlightMyRole(); // 직업 설정 시 테두리 강조

        chatModeCombo.removeAllItems();
        chatModeCombo.addItem("전체");

        if ("Mafia".equals(faction)) {
            chatModeCombo.addItem("마피아");
            chatModeCombo.setVisible(true);
        } else {
            chatModeCombo.setVisible(false);
        }
    }

    // 5. 버튼 클릭 핸들러 (기존 로직 유지)
    private void handlePlayerClick(String targetName) {
        if (isHunterMode) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "정말 '" + targetName + "' 님을 쏘시겠습니까?", "최후의 한 발", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                sendAction(Protocol.CMD_HUNTER_SHOT + " " + targetName);
                isHunterMode = false;
                setTargetSelectionEnabled(false);
                appendMessage("[System] 발포했습니다.");
            }
            return;
        }

        if (amIDead) {
            JOptionPane.showMessageDialog(this, "사망자는 행동할 수 없습니다.");
            return;
        }

        if ("DAY_VOTE".equals(currentPhase)) {
            if ("독재자".equals(myRoleName)) {
                Object[] options = {"투표하기", "쿠데타(능력사용)", "취소"};
                int choice = JOptionPane.showOptionDialog(this,
                        "'" + targetName + "' 님에게 무엇을 하시겠습니까?", "독재자 능력",
                        JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
                        null, options, options[0]);
                if (choice == 0) {
                    sendAction(Protocol.CMD_VOTE + " " + targetName);
                    appendMessage("[System] '" + targetName + "' 님에게 투표했습니다.");
                } else if (choice == 1) {
                    sendAction(Protocol.CMD_DICTATOR_COUP + " " + targetName);
                }
            } else {
                sendAction(Protocol.CMD_VOTE + " " + targetName);
                appendMessage("[System] '" + targetName + "' 님에게 투표했습니다.");
            }
        } else if ("NIGHT_ACTION".equals(currentPhase)) {
            if (myRoleName.equals("큐피드")) {
                handleCupidClick(targetName);
            } else {
                sendAction(Protocol.CMD_NIGHT_ACTION + " " + targetName);
                // 밤 행동은 비밀이므로 시스템 메시지 출력 안 함 (혹은 서버 응답 의존)
            }
        }
    }

    private void handleCupidClick(String targetName) {
        if (cupidTargets.contains(targetName)) {
            cupidTargets.remove(targetName);
            appendMessage("[시스템] 선택 취소: " + targetName);
        } else {
            if (cupidTargets.size() >= 2) {
                JOptionPane.showMessageDialog(this, "두 명까지만 선택할 수 있습니다.");
                return;
            }
            cupidTargets.add(targetName);
            appendMessage("[시스템] 선택: " + targetName);
        }

        if (cupidTargets.size() == 2) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    cupidTargets.toString() + " 연인으로 맺으시겠습니까?", "큐피드", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                String[] t = cupidTargets.toArray(new String[0]);
                sendAction(Protocol.CMD_NIGHT_ACTION + " " + t[0] + " " + t[1]);
                cupidTargets.clear();
            }
        }
    }

    private void sendAction(String message) {
        try {
            if (mainFrame.getSocket() != null) {
                PrintWriter out = new PrintWriter(mainFrame.getSocket().getOutputStream(), true);
                out.println(message);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // 6. 단계 업데이트
    public void updatePhase(String phase, int duration) {
        this.currentPhase = phase;
        this.remainingSeconds = duration;
        this.maxSeconds = duration;

        // Progress Bar 초기화
        if (timerProgressBar != null) {
            timerProgressBar.setMaximum(duration);
            timerProgressBar.setValue(duration);
            timerProgressBar.setString(duration + "초");
            timerProgressBar.setForeground(new Color(100, 200, 100));
        }
        boolean canChat = true;

        if (phase.equals("DAY_DISCUSSION")) {
            phaseLabel.setText("☀ 낮 (토론)");
            phaseLabel.setForeground(new Color(0, 100, 200));
            setTargetSelectionEnabled(false);
            appendMessage("[System] 토론 시간입니다.");
            if (chatModeCombo.isVisible()) chatModeCombo.setSelectedItem("전체");
        } else if (phase.equals("DAY_VOTE")) {
            phaseLabel.setText("🗳 낮 (투표)");
            phaseLabel.setForeground(new Color(200, 50, 0));
            setTargetSelectionEnabled(true);
            appendMessage("[System] 투표 시간입니다.");
        } else if (phase.equals("HUNTER_REVENGE")) {
            phaseLabel.setText("☠️ 사냥꾼의 복수");
            phaseLabel.setForeground(Color.RED);
            setTargetSelectionEnabled(myRoleName.equals("사냥꾼") && amIDead);
            appendMessage("[System] 사냥꾼이 총을 겨누고 있습니다!");
        } else if (phase.equals("NIGHT_ACTION")) {
            phaseLabel.setText("🌙 밤 (능력 사용)");
            phaseLabel.setForeground(new Color(0, 0, 100));
            canChat = "Mafia".equals(myFaction);
            if(canChat) {
                chatModeCombo.setSelectedItem("마피아");
                appendMessage("[System] 마피아 채팅이 활성화되었습니다.");
            } else {
                appendMessage("[System] 밤이 되었습니다.");
            }
            setTargetSelectionEnabled(!myRoleName.equals("시민"));
        }

        if (amIDead) {
            canChat = false;
            setTargetSelectionEnabled(phase.equals("HUNTER_REVENGE") && myRoleName.equals("사냥꾼"));
        }

        setChatEnabled(canChat);

        if (clientTimer != null) clientTimer.stop();

        // 타이머 로직 수정 (프로그레스 바 연동)
        clientTimer = new Timer(1000, e -> {
            remainingSeconds--;
            if (remainingSeconds >= 0) {
                // [수정] 라벨 대신 프로그레스 바 업데이트
                if (timerProgressBar != null) {
                    timerProgressBar.setValue(remainingSeconds);
                    timerProgressBar.setString(remainingSeconds + "초");

                    // 색상 변화 (여유: 초록 -> 60%미만: 노랑 -> 30%미만: 빨강)
                    float ratio = (float) remainingSeconds / maxSeconds;
                    if (ratio < 0.3) {
                        timerProgressBar.setForeground(new Color(220, 50, 50)); // 빨강
                    } else if (ratio < 0.6) {
                        timerProgressBar.setForeground(new Color(220, 180, 50)); // 노랑
                    } else {
                        timerProgressBar.setForeground(new Color(100, 200, 100)); // 초록
                    }
                }
            } else {
                ((Timer)e.getSource()).stop();
            }
        });
        clientTimer.start();
    }

    private void setTargetSelectionEnabled(boolean enabled) {
        // 맵에 있는 모든 버튼 활성/비활성 제어 (사망자 제외)
        for (Map.Entry<String, JButton> entry : playerButtons.entrySet()) {
            if (!deadPlayers.contains(entry.getKey())) {
                entry.getValue().setEnabled(enabled);
            }
        }
    }

    private void setChatEnabled(boolean enabled) {
        chatField.setEditable(enabled);
        if (chatSendButton != null) chatSendButton.setEnabled(enabled);
    }

    // 유틸리티: 사냥꾼 모드
    public void enableHunterMode() {
        this.isHunterMode = true;
        setTargetSelectionEnabled(true);
        JOptionPane.showMessageDialog(this, "사냥꾼 능력 발동! 저승 길동무를 선택하세요.", "알림", JOptionPane.WARNING_MESSAGE);
    }

    // 유틸리티: 연인 채팅
    public void enableLoverChat() {
        boolean hasLover = false;
        for (int i = 0; i < chatModeCombo.getItemCount(); i++) {
            if ("연인".equals(chatModeCombo.getItemAt(i))) hasLover = true;
        }
        if (!hasLover) {
            chatModeCombo.addItem("연인");
            chatModeCombo.setVisible(true);
            appendMessage("[System] 연인 채팅이 활성화되었습니다.");
        }
    }

    public void appendMessage(String msg) {
        chatArea.append(msg + "\n");
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void sendChatMessage() {
        String msg = chatField.getText();
        if (!msg.isEmpty()) {
            try {
                if (mainFrame.getSocket() != null) {
                    PrintWriter out = new PrintWriter(mainFrame.getSocket().getOutputStream(), true);
                    String mode = (String) chatModeCombo.getSelectedItem();
                    String cmd = Protocol.CMD_CHAT;
                    if (chatModeCombo.isVisible()) {
                        if ("마피아".equals(mode)) cmd = Protocol.CMD_MAFIA_CHAT;
                        else if ("유령".equals(mode)) cmd = Protocol.CMD_DEAD_CHAT;
                        else if ("연인".equals(mode)) cmd = Protocol.CMD_LOVER_CHAT;
                    }
                    out.println(cmd + " " + msg);
                    chatField.setText("");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private ImageIcon loadScaledImage(String path, int width, int height) {
        String resourcePath = path.replace("src", "");
        java.net.URL imgURL = getClass().getResource(resourcePath);
        if (imgURL != null) {
            ImageIcon originalIcon = new ImageIcon(imgURL);
            Image img = originalIcon.getImage();
            Image newImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(newImg);
        }
        return null;
    }

    public void reset() {
        deadPlayers.clear();
        amIDead = false;
        currentPhase = "WAITING";
        remainingSeconds = 0;
        survivorCount = 0;
        myRoleName = "시민";
        myFaction = "Citizen";
        playerButtons.clear(); // 버튼 맵 초기화

        chatArea.setText("");
        chatField.setText("");
        if (phaseLabel != null) phaseLabel.setText("게임 대기 중");
        //if (timerLabel != null) timerLabel.setText("-");
        if (survivorCountLabel != null) survivorCountLabel.setText("생존자: 0명");

        if (timerProgressBar != null) {
            timerProgressBar.setValue(0);
            timerProgressBar.setString("-");
            timerProgressBar.setForeground(new Color(100, 200, 100));
        }
        if (chatModeCombo != null) {
            chatModeCombo.removeAllItems();
            chatModeCombo.addItem("전체");
            chatModeCombo.setSelectedItem("전체");
            chatModeCombo.setVisible(false);
            chatModeCombo.setEnabled(true);
        }

        setChatEnabled(true);
        if (playerGridPanel != null) playerGridPanel.removeAll();
        if (roleBookPanel != null) roleBookPanel.removeAll(); // 도감도 초기화

        revalidate();
        repaint();
    }
}