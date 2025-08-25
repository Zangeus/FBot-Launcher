package Config.ConfigWindowStructure;

import Config.LauncherConfig;

import javax.swing.*;
import java.awt.*;

public class TelegramPanel extends JPanel {
    private final LauncherConfig config;
    private JTextField botTokenField;
    private JTextField chatIdField;

    public TelegramPanel(LauncherConfig config) {
        this.config = config;
        initUI();
        loadConfigData();
    }

    private void initUI() {
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        PanelHelper.addHeader(this, "Настройки Telegram", gbc, 0);

        botTokenField = new JTextField(25);
        PanelHelper.addLabeledTextField(this, "Bot Token:", botTokenField, gbc, 1);

        chatIdField = new JTextField(25);
        PanelHelper.addLabeledTextField(this, "Chat ID:", chatIdField, gbc, 2);

        botTokenField.setToolTipText("<html>Токен вашего Telegram бота<br>Пример: 123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11</html>");
        chatIdField.setToolTipText("<html>ID чата для отправки уведомлений<br>Пример: -1001234567890</html>");

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        add(Box.createVerticalGlue(), gbc);
    }

    private void loadConfigData() {
        botTokenField.setText(config.getBotToken());
        chatIdField.setText(config.getChatId());
    }

    public String getBotToken() {
        return botTokenField.getText();
    }

    public String getChatId() {
        return chatIdField.getText();
    }
}