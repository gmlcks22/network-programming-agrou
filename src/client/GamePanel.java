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

public class GamePanel extends JPanel {

    private MainFrame mainFrame;

    // UI 컴포넌트
    private JLabel myRoleImageLabel;
    private JLabel myRoleNameLabel;
    private JLabel survivorCountLabel;

    private JLabel phaseLabel;
    private JLabel timerLabel;

    private JPanel centerDisplayPanel;
    private JLabel roleDescriptionLabel;

    private JTextArea chatArea;
    private JTextField chatField;
    private JButton chatSendButton;
    private JComboBox<String> chatModeCombo;

    private JScrollPane chatScrollPane;
    private JPanel roleBookPanel;

    private JPanel targetSelectionPanel;
    private JPanel playerGridPanel; // 중앙 플레이어 버튼 그리드

    // 데이터
    private String myRoleName = "시민";
    private String myFaction = "Citizen";
    private int survivorCount = 0;

    private String currentPhase = "WAITING";
    private Timer clientTimer;
    private int remainingSeconds = 0;

    // 사망자 관리 및 내 상태
    private Set<String> deadPlayers = new HashSet<>(); // 사망자 목록
    private boolean amIDead = false; // 내가 죽었는지 여부

    // 큐피드
    private boolean isLover = false; // 내가 연인인지 여부
    private Set<String> cupidTargets = new HashSet<>(); // 큐피드용 타겟 저장소
    // 사냥꾼 발포 모드 여부
    private boolean isHunterMode = false;
    // 직업 설명 데이터 (기존 유지)
    private static final Map<String, String> ROLE_DESCRIPTIONS = new HashMap<>();

    static {
        ROLE_DESCRIPTIONS.put("시민", "아무런 능력이 없습니다. 낮 동안의 토론과 투표를 통해 마피아를 찾아내야 합니다.");
        ROLE_DESCRIPTIONS.put("늑대인간", "마피아 진영입니다. 매일 밤 동료들과 상의하여 한 명의 시민을 살해할 수 있습니다.");
        ROLE_DESCRIPTIONS.put("경비병", "매일 밤 자신을 포함한 한 명을 선택하여 늑대인간의 공격으로부터 보호합니다.");
        ROLE_DESCRIPTIONS.put("선견자", "매일 밤 한 명을 선택하여 그 사람의 직업을 알아낼 수 있습니다.");
        ROLE_DESCRIPTIONS.put("마녀", "당신은 일회용 물약 2가지, 치료 물약과 독 물약을 가지고 시작합니다. 치료 물약은 사람을 사망으로부터 지켜낼 수 있고, 독 물약은 사람을 죽입니다.");
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
        initRightPanel();
        initBottomPanel();
    }

    private void initTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. 좌측 (WEST)
        JPanel myRolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        myRolePanel.setOpaque(false);
        myRoleImageLabel = new JLabel();
        myRoleImageLabel.setPreferredSize(new Dimension(50, 50));
        myRoleImageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        myRoleNameLabel = new JLabel("직업: " + myRoleName);
        myRoleNameLabel.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float) 16));
        myRoleNameLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        myRolePanel.add(myRoleImageLabel);
        myRolePanel.add(myRoleNameLabel);
        topPanel.add(myRolePanel, BorderLayout.WEST);

        // 2. 중앙 (CENTER)
        JPanel statusPanel = new JPanel(new GridLayout(2, 1));
        statusPanel.setOpaque(false);
        phaseLabel = new JLabel("게임 대기 중");
        phaseLabel.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float) 18));
        phaseLabel.setHorizontalAlignment(SwingConstants.CENTER);
        timerLabel = new JLabel("-");
        timerLabel.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float) 22));
        timerLabel.setForeground(new Color(200, 50, 50));
        timerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusPanel.add(phaseLabel);
        statusPanel.add(timerLabel);
        topPanel.add(statusPanel, BorderLayout.CENTER);

        // 3. 우측 (EAST)
        survivorCountLabel = new JLabel("생존자: " + survivorCount + "명");
        survivorCountLabel.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float) 20));
        JPanel rightInfoPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        rightInfoPanel.setOpaque(false);
        rightInfoPanel.add(survivorCountLabel);
        topPanel.add(rightInfoPanel, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
    }

    private void initCenterPanel() {
        centerDisplayPanel = new JPanel(new CardLayout());
        centerDisplayPanel.setOpaque(false);
        centerDisplayPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // 플레이어 그리드
        playerGridPanel = new JPanel(new GridLayout(0, 4, 15, 15));
        playerGridPanel.setOpaque(false);
        centerDisplayPanel.add(playerGridPanel, "DEFAULT");

        // 설명 패널
        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.setBorder(BorderFactory.createTitledBorder("직업 설명"));
        roleDescriptionLabel = new JLabel("직업 이미지를 클릭하면 설명이 여기에 표시됩니다.");
        roleDescriptionLabel.setFont(UIManager.getFont("defaultFont").deriveFont(Font.PLAIN, (float) 16));
        roleDescriptionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        descriptionPanel.add(roleDescriptionLabel, BorderLayout.CENTER);

        JButton closeDescButton = new JButton("닫기");
        closeDescButton.addActionListener(e -> showCenterCard("DEFAULT"));
        JPanel closeBtnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        closeBtnPanel.setOpaque(false);
        closeBtnPanel.add(closeDescButton);
        descriptionPanel.add(closeBtnPanel, BorderLayout.SOUTH);

        centerDisplayPanel.add(descriptionPanel, "DESCRIPTION");
        add(centerDisplayPanel, BorderLayout.CENTER);
    }

    private void initRightPanel() {
        targetSelectionPanel = new JPanel();
        targetSelectionPanel.setLayout(new BoxLayout(targetSelectionPanel, BoxLayout.Y_AXIS));
        targetSelectionPanel.setBorder(BorderFactory.createTitledBorder("대상 선택"));
        targetSelectionPanel.setPreferredSize(new Dimension(180, 0));
        setTargetSelectionEnabled(false);
        add(targetSelectionPanel, BorderLayout.EAST);
    }

    private void initBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setPreferredSize(new Dimension(0, 300));

        // 채팅 패널
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("채팅"));
        chatPanel.setPreferredSize(new Dimension(280, 0));
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

        // 도감 패널
        roleBookPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        roleBookPanel.setBorder(BorderFactory.createTitledBorder("직업 도감"));
        JScrollPane bookScrollPane = new JScrollPane(roleBookPanel);
        bookScrollPane.setBorder(null);
        bottomPanel.add(bookScrollPane, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // ================== [기능 구현] ==================
    //  플레이어 사망 처리 (MainFrame에서 호출)
    public void handlePlayerDeath(String deadNickname) {
        // 1. 사망자 목록에 추가
        deadPlayers.add(deadNickname);

        // 2. 생존자 수 감소
        if (survivorCount > 0) {
            survivorCount--;
            survivorCountLabel.setText("생존자: " + survivorCount + "명");
        }

        // 3. 내가 죽었는지 확인
        if (mainFrame.getNickname().equals(deadNickname)) {
            amIDead = true;
            appendMessage("[System] 당신은 사망하여 관전자 상태가 되었습니다.");
            // 죽으면 모든 행동 불가 처리
            setTargetSelectionEnabled(false);
            setChatEnabled(true);

            chatModeCombo.removeAllItems();
            chatModeCombo.addItem("유령");
            chatModeCombo.setSelectedItem("유령");
            chatModeCombo.setVisible(true); // 콤보박스 보이게
            chatModeCombo.setEnabled(false); // 변경 불가능하게 고정
        }

        // 4. UI 갱신 (사망자 회색 처리)
        refreshPlayerGrid();
    }

    // 플레이어 목록 갱신 (사망자 상태 반영)
    public void updateUserList(String[] users) {
        // 초기 생존자 수 설정 (게임 시작 시 한 번만 호출됨을 가정, 혹은 리셋 로직 필요)
        // 여기서는 users 배열 길이 - deadPlayers 크기로 계산
        int currentSurvivors = 0;
        for (String u : users) {
            if (!deadPlayers.contains(u)) {
                currentSurvivors++;
            }
        }
        this.survivorCount = currentSurvivors;
        if (survivorCountLabel != null) {
            survivorCountLabel.setText("생존자: " + survivorCount + "명");
        }

        // 중앙 패널 버튼 다시 그리기
        if (playerGridPanel != null) {
            playerGridPanel.removeAll();

            for (String nickname : users) {
                if (nickname.isEmpty()) {
                    continue;
                }

                JButton playerBtn = new JButton(nickname);
                playerBtn.setFont(UIManager.getFont("defaultFont").deriveFont(Font.BOLD, (float) 16));

                // 사망 여부에 따른 스타일 분기
                if (deadPlayers.contains(nickname)) {
                    playerBtn.setForeground(Color.DARK_GRAY);
                    playerBtn.setEnabled(false); // 클릭 불가
                    playerBtn.setText(nickname + " (사망)");
                } else {
                    playerBtn.setForeground(Color.BLACK);
                    playerBtn.setEnabled(true);
                }

                playerBtn.setPreferredSize(new Dimension(100, 100));
                playerBtn.setFocusPainted(false);
                playerBtn.addActionListener(e -> handlePlayerClick(nickname));

                playerGridPanel.add(playerBtn);
            }
            playerGridPanel.revalidate();
            playerGridPanel.repaint();
        }

        // 우측 타겟 패널도 갱신
        targetSelectionPanel.removeAll();
        for (String user : users) {
            JButton playerBtn = new JButton(user);
            playerBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
            playerBtn.setMaximumSize(new Dimension(160, 40));

            // 사망자는 타겟 목록에서도 비활성화
            if (deadPlayers.contains(user)) {
                playerBtn.setEnabled(false);
                playerBtn.setText(user + " (사망)");
            } else {
                playerBtn.addActionListener(e -> handlePlayerClick(user)); // 클릭 핸들러 통일
            }

            targetSelectionPanel.add(playerBtn);
            targetSelectionPanel.add(Box.createRigidArea(new Dimension(0, 5)));
        }
        targetSelectionPanel.revalidate();
        targetSelectionPanel.repaint();
    }

    // 사망 상태 반영을 위한 그리드 리프레시 (updateUserList 재활용)
    private void refreshPlayerGrid() {
        // MainFrame이나 어딘가에 저장된 전체 유저 리스트가 필요하지만, 
        // 일단 UI 컴포넌트에서 텍스트를 추출해서 다시 그리거나,
        // 간단하게는 버튼들의 상태만 변경할 수도 있습니다.
        // 여기서는 버튼들을 순회하며 상태만 바꿉니다.

        // 중앙 그리드
        for (Component comp : playerGridPanel.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                // 버튼 텍스트에서 닉네임 추출 ( "(사망)"이 안 붙은 상태라고 가정)
                String text = btn.getText();
                String nickname = text.replace(" (사망)", "");

                if (deadPlayers.contains(nickname)) {
                    btn.setEnabled(false);
                    btn.setText(nickname + " (사망)");
                }
            }
        }

        // 우측 타겟 패널
        for (Component comp : targetSelectionPanel.getComponents()) {
            if (comp instanceof JButton) {
                JButton btn = (JButton) comp;
                String text = btn.getText();
                String nickname = text.replace(" (사망)", "");

                if (deadPlayers.contains(nickname)) {
                    btn.setEnabled(false);
                    btn.setText(nickname + " (사망)");
                }
            }
        }

        playerGridPanel.repaint();
        targetSelectionPanel.repaint();
    }

    // 플레이어 버튼 클릭 시 처리
    private void handlePlayerClick(String targetName) {
        // 1. 사냥꾼 모드일 때
        if (isHunterMode) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "정말 '" + targetName + "' 님을 쏘시겠습니까?",
                    "최후의 한 발", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                try {
                    if (mainFrame.getSocket() != null) {
                        PrintWriter out = new PrintWriter(mainFrame.getSocket().getOutputStream(), true);
                        out.println(Protocol.CMD_HUNTER_SHOT + " " + targetName);

                        // 발포 후 모드 해제 및 다시 비활성화
                        isHunterMode = false;
                        setTargetSelectionEnabled(false);
                        appendMessage("[System] 발포했습니다.");
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            return; // 여기서 종료
        }

        // 2. 일반적인 사망자 (행동 불가)        
        if (amIDead) {
            JOptionPane.showMessageDialog(this, "사망자는 행동할 수 없습니다.");
            return;
        }

        try {
            if (mainFrame.getSocket() == null) {
                return;
            }
            PrintWriter out = new PrintWriter(mainFrame.getSocket().getOutputStream(), true);

            if ("DAY_VOTE".equals(currentPhase)) {
                out.println(Protocol.CMD_VOTE + " " + targetName);
                appendMessage("[시스템] '" + targetName + "' 님에게 투표했습니다.");
            } else if ("NIGHT_ACTION".equals(currentPhase)) {

                if (myRoleName.equals("큐피드")) {
                    if (cupidTargets.contains(targetName)) {
                        cupidTargets.remove(targetName); // 선택 해제
                        appendMessage("[시스템] 선택 취소: " + targetName);
                    } else {
                        if (cupidTargets.size() >= 2) {
                            JOptionPane.showMessageDialog(this, "두 명까지만 선택할 수 있습니다. 먼저 선택을 해제하세요.");
                            return;
                        }
                        cupidTargets.add(targetName);
                        appendMessage("[시스템] 선택: " + targetName);
                    }

                    // 2명이 다 선택되었으면 전송 여부 묻기
                    if (cupidTargets.size() == 2) {
                        int confirm = JOptionPane.showConfirmDialog(this,
                                cupidTargets.toString() + " 두 분을 연인으로 맺어주시겠습니까?",
                                "큐피드 능력 사용", JOptionPane.YES_NO_OPTION);

                        if (confirm == JOptionPane.YES_OPTION) {
                            String[] t = cupidTargets.toArray(new String[0]);
                            out.println(Protocol.CMD_NIGHT_ACTION + " " + t[0] + " " + t[1]);
                            cupidTargets.clear(); // 초기화
                        }
                    }
                } else {
                    // 다른 직업은 1명 선택 (기존 로직)
                    out.println(Protocol.CMD_NIGHT_ACTION + " " + targetName);
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void updatePhase(String phase, int duration) {
        this.currentPhase = phase;
        this.remainingSeconds = duration;

        boolean canChat = true;

        if (phase.equals("DAY_DISCUSSION")) {
            phaseLabel.setText("☀ 낮 (토론)");
            phaseLabel.setForeground(new Color(0, 100, 200));
            setTargetSelectionEnabled(false);
            appendMessage("[System] 토론 시간입니다. 자유롭게 대화하세요.");
            canChat = true;
            if (chatModeCombo.isVisible()) {
                chatModeCombo.setSelectedItem("전체");
            }
        } else if (phase.equals("DAY_VOTE")) {
            phaseLabel.setText("🗳 낮 (투표)");
            phaseLabel.setForeground(new Color(200, 50, 0));
            setTargetSelectionEnabled(true);
            appendMessage("[System] 투표 시간입니다. 처형할 대상을 선택하세요.");
            canChat = true;
        } else if (phase.equals("HUNTER_REVENGE")) {
            phaseLabel.setText("☠️ 사냥꾼의 복수");
            phaseLabel.setForeground(Color.RED);

            // 모두 채팅 가능 (살려달라고 빌어야 함)
            canChat = true;
            if (chatModeCombo.isVisible()) {
                chatModeCombo.setSelectedItem("전체");
            }

            if (myRoleName.equals("사냥꾼") && amIDead) {
                // 나는 죽은 사냥꾼이다 -> 타겟 선택 활성화
                setTargetSelectionEnabled(true);
                appendMessage("[System] 당신은 죽었습니다. 제한시간 내에 길동무를 선택하세요!");
            } else {
                // 다른 사람들은 선택 불가
                setTargetSelectionEnabled(false);
                appendMessage("[System] 사냥꾼이 총을 겨누고 있습니다! 채팅으로 설득하세요.");
            }
        } else if (phase.equals("NIGHT_ACTION")) {
            phaseLabel.setText("🌙 밤 (능력 사용)");
            phaseLabel.setForeground(new Color(0, 0, 100));

            if ("Mafia".equals(myFaction)) {
                canChat = true;
                chatModeCombo.setSelectedItem("마피아");
                appendMessage("[System] 마피아들과 은밀하게 대화할 수 있습니다.");
            } else {
                canChat = false;
                appendMessage("[System] 밤이 되었습니다. (채팅 불가)");
            }

            if (myRoleName.equals("시민")) {
                setTargetSelectionEnabled(false);
            } else {
                setTargetSelectionEnabled(true);
            }
        }

        // 내가 죽었으면 채팅, 행동 모두 강제 비활성화
        if (amIDead) {
            canChat = false; // 기본적으로 죽으면 채팅 불가 (유령챗 제외)
            setTargetSelectionEnabled(false);

            // 사냥꾼 페이즈이고 내가 사냥꾼이면 타겟 선택은 가능해야 함
            if (phase.equals("HUNTER_REVENGE") && myRoleName.equals("사냥꾼")) {
                setTargetSelectionEnabled(true);
            }
        }

        setChatEnabled(canChat);

        if (clientTimer != null) {
            clientTimer.stop();
        }
        timerLabel.setText(remainingSeconds + "초");

        clientTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                remainingSeconds--;
                if (remainingSeconds >= 0) {
                    timerLabel.setText(remainingSeconds + "초");
                } else {
                    ((Timer) e.getSource()).stop();
                }
            }
        });
        clientTimer.start();
    }

    private void setChatEnabled(boolean enabled) {
        chatField.setEditable(enabled);
        if (chatSendButton != null) {
            chatSendButton.setEnabled(enabled);
        }
    }

    public void updateRoleBook(String[] roles) {
        roleBookPanel.removeAll();
        Set<String> uniqueRoles = new HashSet<>();
        for (String role : roles) {
            uniqueRoles.add(role.trim());
        }
        for (String roleName : uniqueRoles) {
            addRoleToBook(roleName);
        }
        roleBookPanel.revalidate();
        roleBookPanel.repaint();
    }

    public void setMyRole(String roleName, String faction) {
        this.myRoleName = roleName;
        this.myFaction = faction;
        myRoleNameLabel.setText("직업: " + myRoleName);

        // 이미지 설정 (기존 코드)
        ImageIcon icon = loadScaledImage("src/resources/images/" + roleName + ".png", 50, 50);
        if (icon != null) {
            myRoleImageLabel.setIcon(icon);
        } else {
            myRoleImageLabel.setText(roleName.substring(0, 1));
        }

        // 콤보박스 재설정
        chatModeCombo.removeAllItems();
        chatModeCombo.addItem("전체");

        // 마피아 진영이면 '마피아' 채팅 추가
        if ("Mafia".equals(faction)) {
            chatModeCombo.addItem("마피아");
            chatModeCombo.setVisible(true);
        } else {
            // 시민 등은 특수 채팅이 없으면 콤보박스 숨김 (기본값)
            // 단, 나중에 연인이 되면 다시 보여줘야 함
            chatModeCombo.setVisible(false);
        }
    }

    // 사냥꾼 모드 활성화
    public void enableHunterMode() {
        this.isHunterMode = true;
        // 죽었어도 타겟 선택 가능하게 잠시 활성화
        setTargetSelectionEnabled(true);
        JOptionPane.showMessageDialog(this,
                "당신은 죽었습니다.\n하지만 사냥꾼의 능력으로 저승 길동무를 선택할 수 있습니다!",
                "발포 기회", JOptionPane.WARNING_MESSAGE);

        appendMessage("[System] 총을 쏠 대상을 선택하세요 (제한시간 없음)");
    }

    // 연인이 되었을 때 호출할 메소드
    public void enableLoverChat() {
        // 중복 방지 체크 후 추가
        boolean hasLoverOption = false;
        for (int i = 0; i < chatModeCombo.getItemCount(); i++) {
            if ("연인".equals(chatModeCombo.getItemAt(i))) {
                hasLoverOption = true;
                break;
            }
        }

        if (!hasLoverOption) {
            chatModeCombo.addItem("연인");
            chatModeCombo.setVisible(true); // 콤보박스 활성화
            appendMessage("[System] 연인 채팅 채널이 활성화되었습니다.");
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
                        if ("마피아".equals(mode)) {
                            cmd = Protocol.CMD_MAFIA_CHAT;
                        } else if ("유령".equals(mode)) {
                            cmd = Protocol.CMD_DEAD_CHAT;
                        } else if ("연인".equals(mode)) {
                            cmd = Protocol.CMD_LOVER_CHAT;
                        }
                    }
                    out.println(cmd + " " + msg);
                    chatField.setText("");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void addRoleToBook(String roleName) {
        String imagePath = "src/resources/images/" + roleName + ".png";
        ImageIcon icon = loadScaledImage(imagePath, 60, 90);
        JLabel roleLabel = new JLabel(roleName, SwingConstants.CENTER);
        if (icon != null) {
            roleLabel.setIcon(icon);
            roleLabel.setText("");
            roleLabel.setToolTipText(roleName);
        } else {
            roleLabel.setPreferredSize(new Dimension(60, 90));
            roleLabel.setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY));
        }
        roleLabel.setVerticalTextPosition(JLabel.BOTTOM);
        roleLabel.setHorizontalTextPosition(JLabel.CENTER);
        roleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        roleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showRoleDescription(roleName);
            }
        });
        roleBookPanel.add(roleLabel);
    }

    private void showRoleDescription(String roleName) {
        String description = ROLE_DESCRIPTIONS.getOrDefault(roleName, "설명이 없습니다.");
        roleDescriptionLabel.setText("<html><body style='text-align: center; width: 300px;'>"
                + "<h2>[" + roleName + "]</h2>"
                + "<p>" + description + "</p></body></html>");
        showCenterCard("DESCRIPTION");
    }

    private void showCenterCard(String cardName) {
        CardLayout cl = (CardLayout) centerDisplayPanel.getLayout();
        cl.show(centerDisplayPanel, cardName);
    }

    public void setTargetSelectionEnabled(boolean enabled) {
        for (Component comp : targetSelectionPanel.getComponents()) {
            if (comp instanceof JButton) {
                comp.setEnabled(enabled);
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
        } else {
            return null;
        }
    }

    // 게임 상태 초기화 메소드
    public void reset() {
        // 1. 데이터 초기화
        this.deadPlayers.clear();
        this.amIDead = false;
        this.currentPhase = "WAITING";
        this.remainingSeconds = 0;
        this.survivorCount = 0;
        this.myRoleName = "시민";
        this.myFaction = "Citizen";

        // 2. UI 텍스트 초기화
        chatArea.setText("");
        chatField.setText("");
        if (phaseLabel != null) {
            phaseLabel.setText("게임 대기 중");
        }
        if (timerLabel != null) {
            timerLabel.setText("-");
        }
        if (survivorCountLabel != null) {
            survivorCountLabel.setText("생존자: 0명");
        }

        // 3. 콤보박스 초기화 (전체 채팅으로 복구)
        if (chatModeCombo != null) {
            chatModeCombo.removeAllItems();
            chatModeCombo.addItem("전체");
            chatModeCombo.setSelectedItem("전체");
            chatModeCombo.setVisible(false);
            chatModeCombo.setEnabled(true);
        }

        // 4. 입력창 활성화
        setChatEnabled(true);
        setTargetSelectionEnabled(false);

        // 5. 버튼 패널 비우기
        if (playerGridPanel != null) {
            playerGridPanel.removeAll();
        }
        if (targetSelectionPanel != null) {
            targetSelectionPanel.removeAll();
        }

        revalidate();
        repaint();
    }
}
