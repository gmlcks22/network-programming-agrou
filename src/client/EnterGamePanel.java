package client;

import javax.swing.*;
import java.awt.*;

public class EnterGamePanel extends JPanel {
    private JPanel mainPanel;
    private CardLayout cardLayout;

    public EnterGamePanel(JPanel mainPanel,  CardLayout cardLayout) {
        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;
    }
}
