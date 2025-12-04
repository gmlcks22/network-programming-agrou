package client;

import common.Protocol;
import java.awt.*;
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

    private JPanel centerDisplayPanel; 
    private JLabel roleDescriptionLabel; 

    private JTextArea chatArea;
    private JTextField chatField;
    private JScrollPane chatScrollPane;
    private JPanel roleBookPanel; 

    private JPanel targetSelectionPanel;

    // 데이터
    private String myRoleName = "시민";
    private String myFaction = "Citizen";
    private int survivorCount = 0;

    // 직업 설명 데이터
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

        survivorCountLabel = new JLabel("생존자: " + survivorCount + "명");
        survivorCountLabel.setFont(new Font("맑은 고딕", Font.BOLD, 20));
        survivorCountLabel.setHorizontalAlignment(SwingConstants.CENTER);
        topPanel.add(survivorCountLabel, BorderLayout.CENTER);
        
        topPanel.add(Box.createRigidArea(new Dimension(100, 50)), BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
    }

    private void initCenterPanel() {
        centerDisplayPanel = new JPanel(new CardLayout());
        centerDisplayPanel.setOpaque(false);
        centerDisplayPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JPanel defaultPanel = new JPanel();
        defaultPanel.setOpaque(false);
        centerDisplayPanel.add(defaultPanel, "DEFAULT");

        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.setBackground(new Color(255, 255, 240));
        descriptionPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.ORANGE, 2), "직업 설명",
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("맑은 고딕", Font.BOLD, 18), Color.DARK_GRAY
        ));
        
        roleDescriptionLabel = new JLabel("직업 이미지를 클릭하면 설명이 여기에 표시됩니다.");
        roleDescriptionLabel.setFont(new Font("맑은 고딕", Font.PLAIN, 16));
        roleDescriptionLabel.setHorizontalAlignment(SwingConstants.CENTER);
        roleDescriptionLabel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
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
        targetSelectionPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), "능력 대상 선택",
                TitledBorder.CENTER, TitledBorder.TOP,
                new Font("맑은 고딕", Font.BOLD, 14)
        ));
        targetSelectionPanel.setPreferredSize(new Dimension(180, 0));

        setTargetSelectionEnabled(false);
        add(targetSelectionPanel, BorderLayout.EAST);
    }

    private void initBottomPanel() {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        // 하단 패널 
        bottomPanel.setPreferredSize(new Dimension(0, 300)); 

        // 1) 좌측: 채팅창 패널
        JPanel chatPanel = new JPanel(new BorderLayout());
        chatPanel.setBorder(BorderFactory.createTitledBorder("채팅"));
        chatPanel.setPreferredSize(new Dimension(280, 0)); 

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setLineWrap(true);
        chatScrollPane = new JScrollPane(chatArea);
        chatPanel.add(chatScrollPane, BorderLayout.CENTER);

        JPanel inputPanel = new JPanel(new BorderLayout());
        chatField = new JTextField();
        chatField.addActionListener(e -> sendChatMessage());
        JButton sendButton = new JButton("전송");
        sendButton.addActionListener(e -> sendChatMessage());
        
        inputPanel.add(chatField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        chatPanel.add(inputPanel, BorderLayout.SOUTH);

        bottomPanel.add(chatPanel, BorderLayout.WEST);

        // 2) 중앙: 직업 도감 패널
        roleBookPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        roleBookPanel.setBorder(BorderFactory.createTitledBorder(
                null, "직업 도감 (이번 판 등장 직업)", TitledBorder.CENTER, TitledBorder.TOP));
        roleBookPanel.setBackground(new Color(250, 250, 245));

        // 초기에는 비워둡니다 (서버에서 목록을 받아와서 채움)
        
        JScrollPane bookScrollPane = new JScrollPane(roleBookPanel);
        bookScrollPane.setBorder(null);
        bottomPanel.add(bookScrollPane, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.SOUTH);
    }

    // --- 기능 메소드 ---

    // 서버로부터 받은 직업 목록으로 도감 업데이트
    public void updateRoleBook(String[] roles) {
        roleBookPanel.removeAll(); // 기존 내용 초기화
        
        // 중복 제거를 위해 Set 사용 (같은 직업이 여러 명이어도 도감에는 하나만 표시)
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

        String imagePath = "src/resources/images/" + roleName + ".png";
        ImageIcon icon = loadScaledImage(imagePath, 50, 50);
        if (icon != null) {
            myRoleImageLabel.setIcon(icon);
        } else {
            myRoleImageLabel.setText(roleName.substring(0, 1));
        }
    }

    public void updateUserList(String[] users) {
        this.survivorCount = users.length;
        survivorCountLabel.setText("생존자: " + survivorCount + "명");
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
                    out.println(Protocol.CMD_CHAT + " " + msg);
                    chatField.setText("");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    // 도감에 아이콘 추가 (이미지 파일명은 직업명.png로 가정)
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
            comp.setEnabled(enabled);
        }
    }

    private ImageIcon loadScaledImage(String path, int width, int height) {
        String resourcePath = path.replace("src", ""); // "src" 제거
        
        // 리소스 URL 가져오기
        java.net.URL imgURL = getClass().getResource(resourcePath);
        
        if (imgURL != null) {
            ImageIcon originalIcon = new ImageIcon(imgURL);
            Image img = originalIcon.getImage();
            Image newImg = img.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            return new ImageIcon(newImg);
        } else {
            System.err.println("이미지를 찾을 수 없습니다: " + resourcePath);
            return null;
        }
    }
}