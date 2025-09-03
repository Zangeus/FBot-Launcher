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
    private JTextField starRailCopilotField;

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

        PanelHelper.addHeader(this, "Настройки путей", gbc, 0);

        // Поле для изображений
        picsPathField = new JTextField(25);
        PanelHelper.addLabeledTextField(this, "Путь к изображениям:", picsPathField, gbc, 1);
        addBrowseButton(picsPathField, gbc, 1);

        // Поле для StarRailCopilot
        starRailCopilotField = new JTextField(25);
        PanelHelper.addLabeledTextField(this, "Путь к StarRailCopilot:", starRailCopilotField, gbc, 2);
        addBrowseButton(starRailCopilotField, gbc, 2);

        // Кнопка "открыть руководство"
        JButton openReadmeBtn = new JButton("Открыть руководство");
        StyleManager.styleButton(openReadmeBtn, StyleManager.WARNING_COLOR, Color.WHITE);
        openReadmeBtn.setPreferredSize(new Dimension(200, 35));
        openReadmeBtn.addActionListener(this::openReadme);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(openReadmeBtn, gbc);

        // Разделитель
        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(new Color(220, 220, 220));
        gbc.gridy = 4;
        gbc.insets = new Insets(20, 0, 10, 0);
        add(separator, gbc);

        gbc.gridy = 5;
        gbc.weighty = 1.0;
        add(Box.createGlue(), gbc);
    }

    private void addBrowseButton(JTextField targetField, GridBagConstraints gbc, int row) {
        JButton browseBtn = new JButton("Обзор...");
        StyleManager.styleButton(browseBtn, StyleManager.PRIMARY_COLOR, Color.WHITE);
        browseBtn.setPreferredSize(new Dimension(120, 35));
        browseBtn.addActionListener(e -> browseDirectory(targetField));
        gbc.gridx = 2;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        add(browseBtn, gbc);
    }

    private void browseDirectory(JTextField targetField) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            targetField.setText(chooser.getSelectedFile().getAbsolutePath());
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
                    JOptionPane.ERROR_MESSAGE);
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
        starRailCopilotField.setText(config.getStarRailCopilotPath());
    }

    public String getPicsPath() {
        return picsPathField.getText();
    }

    public String getStarRailCopilotPath() {
        return starRailCopilotField.getText();
    }
}
