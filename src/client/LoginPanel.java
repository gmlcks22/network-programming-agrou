package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginPanel extends JPanel {
    private MainFrame mainFrame;

    // GUI 컴포넌트 선언
    private JTextField ipField;
    private JTextField portField;
    private JTextField nicknameField;
    private JButton loginButton;

    // 생성자(Constructor)
    public LoginPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;

        // 기본 설정 (백그라운드, 전체 레이아웃)
        this.setBackground(Color.WHITE);
        this.setLayout(new BorderLayout(10, 10)); // 전체 레이아웃 (북, 중앙)
        this.setBorder(BorderFactory.createEmptyBorder(50, 50, 50, 50));

        // 타이틀 (North)
        JLabel titleLabel = new JLabel("Mafia Game");
        titleLabel.setFont(new Font("SansSerif", Font.BOLD, 40));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        add(titleLabel, BorderLayout.NORTH);

        // 폼 패널 (Center)
        JPanel formWrapperPanel = new JPanel(new GridBagLayout());
        formWrapperPanel.setBackground(Color.WHITE); // 배경색 통일

        // 실제 폼 컴포넌트(필드, 버튼)들을 담을 패널 (Y축으로 쌓음)
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setBackground(Color.WHITE);

        // 필드 및 버튼 생성
        ipField = new JTextField(20);
        portField = new JTextField(20);
        nicknameField = new JTextField(20);
        loginButton = new JButton("로그인");

        // Placeholder 텍스트 구현
        addPlaceholder(ipField, "서버 ip");
        addPlaceholder(portField, "포트번호");
        addPlaceholder(nicknameField, "닉네임");

        // 컴포넌트 크기 설정 (일관된 크기)
        Dimension fieldSize = new Dimension(250, 40);
        ipField.setPreferredSize(fieldSize);
        ipField.setMaximumSize(fieldSize); // BoxLayout을 위해
        portField.setPreferredSize(fieldSize);
        portField.setMaximumSize(fieldSize);
        nicknameField.setPreferredSize(fieldSize);
        nicknameField.setMaximumSize(fieldSize);
        loginButton.setPreferredSize(fieldSize);
        loginButton.setMaximumSize(fieldSize);
        
        // 폰트 및 정렬 설정
        Font fieldFont = new Font("SansSerif", Font.PLAIN, 14);
        ipField.setFont(fieldFont);
        portField.setFont(fieldFont);
        nicknameField.setFont(fieldFont);
        
        loginButton.setBackground(new Color(220, 220, 220)); // 이미지와 유사한 회색
        loginButton.setFont(new Font("SansSerif", Font.BOLD, 14));

        // 폼 패널에 컴포넌트 추가
        formPanel.add(ipField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10))); // 컴포넌트 사이 간격
        formPanel.add(portField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 10))); // 간격
        formPanel.add(nicknameField);
        formPanel.add(Box.createRigidArea(new Dimension(0, 20))); // 버튼 전 간격
        formPanel.add(loginButton);

        // 컴포넌트들 중앙 정렬
        ipField.setAlignmentX(Component.CENTER_ALIGNMENT);
        portField.setAlignmentX(Component.CENTER_ALIGNMENT);
        nicknameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // formWrapperPanel(GridBagLayout)에 formPanel을 추가 (패널 자체가 중앙에 감)
        GridBagConstraints gbc = new GridBagConstraints();
        formWrapperPanel.add(formPanel, gbc); // gbc 기본값이 중앙

        // 전체 LoginPanel의 CENTER에 formWrapperPanel을 추가
        this.add(formWrapperPanel, BorderLayout.CENTER);

        // "로그인" 버튼 액션 리스너
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tryConnect();
            }
        });
    }

    // 서버 연결 로직
    private void tryConnect() {
        String ip = ipField.getText().trim();
        String portStr = portField.getText().trim();
        String nickname = nicknameField.getText().trim();

        // 입력값 검증
        if (nickname.isEmpty()) {
            JOptionPane.showMessageDialog(this, "닉네임을 입력해주세요.", "경고", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (ip.isEmpty()) {
            JOptionPane.showMessageDialog(this, "서버 IP를 입력해주세요.", "경고", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (portStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "포트 번호를 입력해주세요.", "경고", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int port = Integer.parseInt(portStr);
            Socket socket = new Socket(ip, port);
            
            // 소켓 연결 직후 스트림을 열어 통신 시작
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            
            // 서버로 닉네임 전송
            out.println(nickname);
            
            // 서버 응답 대기("OK" or "FAIL")
            String response = in.readLine();
            
            if ("OK".equals(response)) {
                // IP, 포트, 닉네임을 함께 출력하도록 변경
                System.out.println("[Client] 서버 접속 성공! (IP: " + ip + ", Port: " + port + ", User: " + nickname + ")");
                mainFrame.connectSuccess(socket, nickname);
                mainFrame.changePanel(MainFrame.LOBBY_PANEL);
            }else {
                JOptionPane.showMessageDialog(this, "이미 사용 중인 닉네임입니다.");
                socket.close(); // 실패했으니 소켓 닫기
            }
            
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "포트 번호는 숫자여야 합니다.", "오류", JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "서버 연결 실패!\nIP와 포트를 다시 확인해주세요.", "연결 오류", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * (부가 기능) JTextField에 Placeholder 텍스트를 추가하는 헬퍼 메소드
     */
    private void addPlaceholder(JTextField textField, String placeholder) {
        textField.setText(placeholder);
        textField.setForeground(Color.GRAY);

        textField.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                // 클릭해서 포커스를 얻었을 때
                if (textField.getText().equals(placeholder)) {
                    textField.setText("");
                    textField.setForeground(Color.BLACK);
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                // 포커스를 잃었을 때
                if (textField.getText().isEmpty()) {
                    textField.setText(placeholder);
                    textField.setForeground(Color.GRAY);
                }
            }
        });
    }
}