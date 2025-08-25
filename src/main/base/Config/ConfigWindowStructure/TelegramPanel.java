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

        JLabel header = new JLabel("Настройки Telegram");
        header.setFont(StyleManager.HEADER_FONT.deriveFont(18f));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(header, gbc);

        JLabel tokenLabel = new JLabel("Bot Token:");
        tokenLabel.setFont(StyleManager.BASE_FONT);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        add(tokenLabel, gbc);

        botTokenField = new JTextField(25);
        botTokenField.setFont(StyleManager.BASE_FONT);
        botTokenField.setBorder(StyleManager.createTextFieldBorder());
        gbc.gridx = 1;
        add(botTokenField, gbc);

        JLabel chatIdLabel = new JLabel("Chat ID:");
        chatIdLabel.setFont(StyleManager.BASE_FONT);
        gbc.gridx = 0;
        gbc.gridy = 2;
        add(chatIdLabel, gbc);

        chatIdField = new JTextField(25);
        chatIdField.setFont(StyleManager.BASE_FONT);
        chatIdField.setBorder(StyleManager.createTextFieldBorder());
        gbc.gridx = 1;
        add(chatIdField, gbc);

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