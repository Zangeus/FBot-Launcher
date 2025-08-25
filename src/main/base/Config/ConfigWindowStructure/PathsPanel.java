package Config.ConfigWindowStructure;

import Config.LauncherConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class PathsPanel extends JPanel {
    private final LauncherConfig config;
    private JTextField picsPathField;

    public PathsPanel(LauncherConfig config) {
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

        // Заголовок
        JLabel header = new JLabel("Настройки путей");
        header.setFont(StyleManager.HEADER_FONT.deriveFont(18f));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(header, gbc);

        // Поле для пути к изображениям
        JLabel pathLabel = new JLabel("Путь к изображениям:");
        pathLabel.setFont(StyleManager.BASE_FONT);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        add(pathLabel, gbc);

        picsPathField = new JTextField(25);
        picsPathField.setFont(StyleManager.BASE_FONT);
        picsPathField.setBorder(StyleManager.createTextFieldBorder());
        gbc.gridx = 1;
        add(picsPathField, gbc);

        // Кнопка "Обзор"
        JButton browseBtn = new JButton("Обзор...");
        StyleManager.styleButton(browseBtn, StyleManager.PRIMARY_COLOR, Color.WHITE);
        browseBtn.setPreferredSize(new Dimension(120, 35));
        browseBtn.addActionListener(this::browseDirectory);
        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        add(browseBtn, gbc);

        // Кнопка "Открыть руководство"
        JButton openReadmeBtn = new JButton("Открыть руководство");
        StyleManager.styleButton(openReadmeBtn, StyleManager.WARNING_COLOR, Color.WHITE);
        openReadmeBtn.setPreferredSize(new Dimension(200, 35));
        openReadmeBtn.addActionListener(this::openReadme);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(openReadmeBtn, gbc);

        // Разделитель
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(new Color(220, 220, 220));
        gbc.gridy = 3;
        gbc.insets = new Insets(20, 0, 10, 0);
        add(separator, gbc);

        // Пустое пространство
        gbc.gridy = 4;
        gbc.weighty = 1.0;
        add(Box.createGlue(), gbc);
    }

    private void browseDirectory(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            picsPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void openReadme(ActionEvent e) {
        try {
            File readmeFile = new File(config.getReadmePath());
            if (!readmeFile.exists()) {
                createDefaultReadme(readmeFile);
            }
            Desktop.getDesktop().open(readmeFile);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "Ошибка при открытии README: " + ex.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE); // :cite[4]:cite[6]
        }
    }

    private void createDefaultReadme(File readmeFile) throws IOException {
        try (java.io.FileWriter writer = new java.io.FileWriter(readmeFile)) {
            writer.write("# Руководство пользователя\n\n");
            writer.write("Основные файлы конфигурации должны находиться в папке `bot_sources`\n");
            writer.write("Полная документация доступна по [ссылке](google.com)");
        }
    }

    private void loadConfigData() {
        picsPathField.setText(config.getPicsToStartPath());
    }

    public String getPicsPath() {
        return picsPathField.getText();
    }
}