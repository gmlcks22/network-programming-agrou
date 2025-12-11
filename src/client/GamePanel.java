package client;

import common.Protocol;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
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
    
    // 직업 설명 데이터 (기존 유지)
    private static final Map<String, String> ROLE_DESCRIPTIONS = new HashMap<>();
    static {
        ROLE_DESCRIPTIONS.put("시민", "아무런 능력이 없습니다. 낮 동안의 토론과 투표를 통해 마피아를 찾아내야 합니다.");
        ROLE_DESCRIPTIONS.put("늑대인간", "마피아 진영입니다. 매일 밤 동료들과 상의하여 한 명의 시민을 살해할 수 있습니다.");
        ROLE_DESCRIPTIONS.put("경비병", "매일 밤 자신을 포함한 한 명을 선택하여 늑대인간의 공격으로부터 보호합니다.");
        ROLE_DESCRIPTIONS.put("선견자", "매일 밤 한 명을 선택하여 그 사람의 직업을 알아낼 수 있습니다.");
        ROLE_DESCRIPTIONS.put("마녀", "게임 중 각 한 번씩, 사람을 살리는 약과 죽이는 약을 사용할 수 있습니다.");
        ROLE_DESCRIPTIONS.put("사냥꾼", "자신이 사망할 때, 유언으로 다른 한 명을 지목하여 함께 데려갈 수 있습니다.");
        ROLE_DESCRIPTIONS.put("독재자", "투표 시간에 능력을 사용하여 혼자서 처형 대상을 결정할 수 있습니다. (1회)");
        ROLE_DESCRIPTIONS.put("천사", "첫날 낮 투표에서 처형당하면 즉시 게임에서 승리합니다.");
        ROLE_DESCRIPTIONS.put("큐피드", "첫날 밤 두 명을 연인으로 지정합니다. 한 명이 죽으면 다른 한 명도 함께 죽습니다.");
    }

    public GamePanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setBackground(new Color(245, 245, 250));

        initTopPanel();
        initCenterPanel();
        initRightPanel();
        initBottomPanel();
    }

    private void initTopPanel() {
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(new Color(230, 230, 240));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 1. 좌측 (WEST)
        JPanel myRolePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        myRolePanel.setOpaque(false);
        myRoleImageLabel = new JLabel();
        myRoleImageLabel.setPreferredSize(new Dimension(50, 50));
        myRoleImageLabel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        myRoleNameLabel = new JLabel("직업: " + myRoleName);
        myRoleNameLabel.setFont(new Font("맑은 고딕", Font.BOLD, 16));
        myRoleNameLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        myRolePanel.add(myRoleImageLabel);
        myRolePanel.add(myRoleNameLabel);
        topPanel.add(myRolePanel, BorderLayout.WEST);

        // 2. 중앙 (CENTER)
        JPanel statusPanel = new JPanel(new GridLayout(2, 1));
        statusPanel.setOpaque(false);
        phaseLabel = new JLabel("게임 대기 중");
        phaseLabel.setFont(new Font("맑은 고딕", Font.BOLD, 18));
        phaseLabel.setHorizontalAlignment(SwingConstants.CENTER);
        timerLabel = new JLabel("-");
        timerLabel.setFont(new Font("맑은 고딕", Font.BOLD, 22));
        timerLabel.setForeground(new Color(200, 50, 50));
        timerLabel.setHorizontalAlignment(SwingConstants.CENTER);
        statusPanel.add(phaseLabel);
        statusPanel.add(timerLabel);
        topPanel.add(statusPanel, BorderLayout.CENTER);

        // 3. 우측 (EAST)
        survivorCountLabel = new JLabel("생존자: " + survivorCount + "명");
        survivorCountLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
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
        descriptionPanel.setBackground(new Color(255, 255, 240));
        descriptionPanel.setBorder(BorderFactory.createTitledBorder("직업 설명"));
        roleDescriptionLabel = new JLabel("직업 이미지를 클릭하면 설명이 여기에 표시됩니다.");
        roleDescriptionLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
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
        targetSelectionPanel.setBackground(new Color(240, 245, 255));
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
        chatModeCombo = new JComboBox<>(new String[]{"전체", "마피아"});
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
        roleBookPanel.setBackground(new Color(250, 250, 245));
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
        for(String u : users) {
            if(!deadPlayers.contains(u)) currentSurvivors++;
        }
        this.survivorCount = currentSurvivors;
        if(survivorCountLabel != null) {
            survivorCountLabel.setText("생존자: " + survivorCount + "명");
        }

        // 중앙 패널 버튼 다시 그리기
        if (playerGridPanel != null) {
            playerGridPanel.removeAll();    

            for (String nickname : users) {
                if (nickname.isEmpty()) continue;

                JButton playerBtn = new JButton(nickname);
                playerBtn.setFont(new Font("맑은 고딕", Font.BOLD, 16));
                
                // 사망 여부에 따른 스타일 분기
                if (deadPlayers.contains(nickname)) {
                    playerBtn.setBackground(Color.GRAY); // 회색 배경
                    playerBtn.setForeground(Color.DARK_GRAY);
                    playerBtn.setEnabled(false); // 클릭 불가
                    playerBtn.setText(nickname + " (사망)");
                } else {
                    playerBtn.setBackground(new Color(220, 230, 255)); // 생존자 색상
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
            playerBtn.setBackground(Color.WHITE);
            
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
                    btn.setBackground(Color.GRAY);
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
        // ★ 내가 죽었으면 아무것도 못함
        if (amIDead) {
            JOptionPane.showMessageDialog(this, "사망자는 행동할 수 없습니다.");
            return;
        }

        try {
            if (mainFrame.getSocket() == null) return;
            PrintWriter out = new PrintWriter(mainFrame.getSocket().getOutputStream(), true);

            if ("DAY_VOTE".equals(currentPhase)) {
                out.println(Protocol.CMD_VOTE + " " + targetName);
                appendMessage("[시스템] '" + targetName + "' 님에게 투표했습니다.");
            }
            else if ("NIGHT_ACTION".equals(currentPhase)) {
                out.println(Protocol.CMD_NIGHT_ACTION + " " + targetName);
                // appendMessage("[시스템] '" + targetName + "' 님을 선택했습니다.");
            }
            else {
                JOptionPane.showMessageDialog(this, "지금은 대상을 선택할 수 없습니다.");
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
            if (chatModeCombo.isVisible()) chatModeCombo.setSelectedItem("전체");
        } 
        else if (phase.equals("DAY_VOTE")) {
            phaseLabel.setText("🗳 낮 (투표)");
            phaseLabel.setForeground(new Color(200, 50, 0)); 
            setTargetSelectionEnabled(true); 
            appendMessage("[System] 투표 시간입니다. 처형할 대상을 선택하세요.");
            canChat = true;
        } 
        else if (phase.equals("NIGHT_ACTION")) {
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

            if (myRoleName.equals("시민")) setTargetSelectionEnabled(false);
            else setTargetSelectionEnabled(true);
        }

        // 내가 죽었으면 채팅, 행동 모두 강제 비활성화
        if (amIDead) {
            canChat = false;
            setTargetSelectionEnabled(false);
        }

        setChatEnabled(canChat);

        if (clientTimer != null) clientTimer.stop();
        timerLabel.setText(remainingSeconds + "초");
        
        clientTimer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                remainingSeconds--;
                if (remainingSeconds >= 0) timerLabel.setText(remainingSeconds + "초");
                else ((Timer)e.getSource()).stop();
            }
        });
        clientTimer.start();
    }

    private void setChatEnabled(boolean enabled) {
        chatField.setEditable(enabled);
        if (chatSendButton != null) chatSendButton.setEnabled(enabled);
    }

    public void updateRoleBook(String[] roles) {
        roleBookPanel.removeAll();
        Set<String> uniqueRoles = new HashSet<>();
        for (String role : roles) uniqueRoles.add(role.trim());
        for (String roleName : uniqueRoles) addRoleToBook(roleName);
        roleBookPanel.revalidate();
        roleBookPanel.repaint();
    }

    public void setMyRole(String roleName, String faction) {
        this.myRoleName = roleName;
        this.myFaction = faction;
        myRoleNameLabel.setText("직업: " + myRoleName);
        ImageIcon icon = loadScaledImage("src/resources/images/" + roleName + ".png", 50, 50);
        if (icon != null) myRoleImageLabel.setIcon(icon);
        else myRoleImageLabel.setText(roleName.substring(0, 1));
        
        if ("Mafia".equals(faction)) {
            chatModeCombo.setVisible(true); 
            chatModeCombo.setSelectedIndex(0); 
        } else {
            chatModeCombo.setVisible(false);
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
                    if (chatModeCombo.isVisible() && "마피아".equals(mode)) {
                        cmd = Protocol.CMD_MAFIA_CHAT;
                    }
                    else if ("유령".equals(mode)) {
                            cmd = Protocol.CMD_DEAD_CHAT;
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
        roleDescriptionLabel.setText("<html><body style='text-align: center; width: 300px;'>" + 
                                     "<h2>[" + roleName + "]</h2>" + 
                                     "<p>" + description + "</p></body></html>");
        showCenterCard("DESCRIPTION");
    }

    private void showCenterCard(String cardName) {
        CardLayout cl = (CardLayout) centerDisplayPanel.getLayout();
        cl.show(centerDisplayPanel, cardName);
    }

    public void setTargetSelectionEnabled(boolean enabled) {
        for (Component comp : targetSelectionPanel.getComponents()) {
            if (comp instanceof JButton) comp.setEnabled(enabled);
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
}