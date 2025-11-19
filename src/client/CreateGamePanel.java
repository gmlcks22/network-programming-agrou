package client;

import javax.swing.*;
import java.awt.*;

public class CreateGamePanel extends JPanel {
    private JPanel mainPanel;
    private CardLayout cardLayout;

    public CreateGamePanel(JPanel mainPanel,  CardLayout cardLayout) {
        this.mainPanel = mainPanel;
        this.cardLayout = cardLayout;
    }
}
