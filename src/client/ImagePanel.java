package client;

import javax.swing.*;
import java.awt.*;

// 이미지 배경용
public class ImagePanel extends JPanel {
    private Image img;

    public ImagePanel(String imagePath) {
        // 이미지 로드 (리소스 폴더 기준)
        // 예: new ImageIcon(getClass().getResource(imagePath)).getImage();
        try {
            ImageIcon icon = new ImageIcon(getClass().getResource(imagePath));
            this.img = icon.getImage();
        } catch (Exception e) {
            System.err.println("배경 이미지를 찾을 수 없습니다: " + imagePath);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        // 이미지가 있을 때만 그림
        if (img != null) {
            // 화면 크기에 꽉 차게 그림 (Stretch)
            g.drawImage(img, 0, 0, getWidth(), getHeight(), this);
        }
    }
}
