package Config;

import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;

import Waiters.Main;

public class ConfigWindow extends JFrame {
    private final LauncherConfig config = ConfigManager.loadConfig();
    private final Color middleGray = Color.WHITE;//new Color(50, 50, 50);

    private Point lastSaveClickPoint;
    private JTextField botTokenField;
    private JTextField chatIdField;
    private JTextField picsPathField;
    private final JTextArea successMessagesArea;
    private final JTextArea failureMessagesArea;
    private final JTextArea reportMessagesArea;
    private JSpinner attemptsSpinner;
    private JCheckBox successCheck;
    private JCheckBox failureCheck;
    private JCheckBox reportCheck;
    private JCheckBox mondayCheck;
    private JSpinner sleepDurationSpinner;

    private JLabel monitoringStatusLabel = new JLabel();
    private JButton monitoringToggleButton = new JButton();

    private static final Font SMALLER_FONT = new Font("Segoe UI", Font.PLAIN, 12);

    private static final Color PRIMARY_COLOR = new Color(70, 130, 180);
    private static final Color DANGER_COLOR = new Color(220, 80, 80);
    private static final Color WARNING_COLOR = new Color(255, 165, 0);
    private static final Color TEXT_COLOR = new Color(50, 50, 50);

    // Шрифты
    private static final Font BASE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font HEADER_FONT = new Font("Segoe UI SemiBold", Font.BOLD, 16);
    private static final Font TITLE_FONT = new Font("Segoe UI SemiBold", Font.BOLD, 18);
    private static final Font BUTTON_FONT = new Font("Segoe UI SemiBold", Font.BOLD, 14);

    private static final int MIN_WIDTH = 800;
    private static final int MAX_WIDTH = 950;
    private static final int PREF_HEIGHT = 600;

    public ConfigWindow() {
        botTokenField = new JTextField();
        chatIdField = new JTextField();
        picsPathField = new JTextField();
        successMessagesArea = new JTextArea();
        failureMessagesArea = new JTextArea();
        reportMessagesArea = new JTextArea();
        loadCustomFont();
        initUI();
    }

    private void initUI() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 5);
            UIManager.put("Button.font", BUTTON_FONT);
            UIManager.put("Label.font", BASE_FONT);
            UIManager.put("TextField.font", BASE_FONT);
            UIManager.put("TextArea.font", BASE_FONT);
            UIManager.put("Spinner.font", BASE_FONT);
            UIManager.put("CheckBox.font", BASE_FONT);
            UIManager.put("TabbedPane.font", TITLE_FONT);

        } catch (Exception ignored) {
        }

        setTitle("Настройки");

        setLayout(new BorderLayout());
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        //setExtendedState(JFrame.MAXIMIZED_BOTH);
        setUndecorated(true);
        setLocationRelativeTo(null);

        ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icon.png")));
        if (icon.getImage() != null) {
            setIconImage(icon.getImage());
        }


        ImageIcon bgIcon = new ImageIcon(new File(
                config.getPicsToStartPath() + "\\" + "background.png").getAbsolutePath());
        Image backgroundImage = bgIcon.getImage();

        int imageWidth = (backgroundImage != null) ? bgIcon.getIconWidth() : 0;
        int calculatedWidth = Math.min(Math.max(MIN_WIDTH, imageWidth), MAX_WIDTH);

        setSize(calculatedWidth, PREF_HEIGHT);
        setPreferredSize(new Dimension(calculatedWidth, PREF_HEIGHT));
        setMinimumSize(new Dimension(MIN_WIDTH, PREF_HEIGHT));
        setMaximumSize(new Dimension(MAX_WIDTH, PREF_HEIGHT));
        setResizable(false);

        setLocationRelativeTo(null);

        BackgroundPanel bgPanel = new BackgroundPanel(bgIcon.getImage(), 0.75f);
        bgPanel.setOpaque(true);
        setContentPane(bgPanel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (!Main.isRunning()) {
                    System.exit(0);
                }
            }
        });


        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(HEADER_FONT);
        tabbedPane.addTab("Основные", createGeneralPanel());
        tabbedPane.addTab("Telegram", createTelegramPanel());
        tabbedPane.addTab("Пути", createPathsPanel());
        tabbedPane.addTab("Сообщения", createMessagesPanel());
        tabbedPane.setOpaque(true);

        add(tabbedPane, BorderLayout.CENTER);
        add(createButtonPanel(), BorderLayout.SOUTH);

        bgPanel.setOpaque(true);
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(true);
        centerPanel.add(tabbedPane, BorderLayout.CENTER);
        bgPanel.add(centerPanel, BorderLayout.CENTER);
        bgPanel.add(createButtonPanel(), BorderLayout.SOUTH);

        bgPanel.add(createButtonPanel(), BorderLayout.SOUTH);
        botTokenField.setText(config.getBotToken());
        chatIdField.setText(config.getChatId());
        picsPathField.setText(config.getPicsToStartPath());
        successMessagesArea.setText(String.join("\n", config.getSuccessMessages()));
        failureMessagesArea.setText(String.join("\n", config.getFailureMessages()));
        reportMessagesArea.setText(String.join("\n", config.getReportMessages()));
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        panel.setOpaque(true);

        JButton saveBtn = new JButton("Сохранить");
        styleButton(saveBtn, new Color(15, 157, 88), Color.WHITE);
        saveBtn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                lastSaveClickPoint = e.getPoint();
                lastSaveClickPoint = SwingUtilities.convertPoint(saveBtn, lastSaveClickPoint, ConfigWindow.this);
            }
        });
        saveBtn.addActionListener(this::saveConfig);

        JButton cancelBtn = new JButton("Отмена");
        styleButton(cancelBtn, new Color(66, 133, 244), Color.WHITE);
        cancelBtn.addActionListener(e -> dispose());

        panel.add(saveBtn);
        panel.add(cancelBtn);
        return panel;
    }

    private JPanel createTelegramPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel header = new JLabel("Настройки Telegram");
        header.setFont(HEADER_FONT.deriveFont(18f));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(header, gbc);

        JLabel tokenLabel = new JLabel("Bot Token:");
        tokenLabel.setFont(BASE_FONT);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        panel.add(tokenLabel, gbc);

        botTokenField = new JTextField(config.getBotToken(), 25);
        botTokenField.setFont(BASE_FONT);
        botTokenField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        gbc.gridx = 1;
        panel.add(botTokenField, gbc);

        JLabel chatIdLabel = new JLabel("Chat ID:");
        chatIdLabel.setFont(BASE_FONT);
        gbc.gridx = 0;
        gbc.gridy = 2;
        panel.add(chatIdLabel, gbc);

        chatIdField = new JTextField(config.getChatId(), 25);
        chatIdField.setFont(BASE_FONT);
        chatIdField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        gbc.gridx = 1;
        panel.add(chatIdField, gbc);

        botTokenField.setToolTipText("<html>Токен вашего Telegram бота<br>Пример: 123456:ABC-DEF1234ghIkl-zyx57W2v1u123ew11</html>");
        chatIdField.setToolTipText("<html>ID чата для отправки уведомлений<br>Пример: -1001234567890</html>");

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        gbc.weighty = 1.0;
        panel.add(Box.createVerticalGlue(), gbc);

        return panel;
    }

    private JPanel createMessagesPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));

        JTabbedPane messagesTabbedPane = new JTabbedPane();
        messagesTabbedPane.setFont(HEADER_FONT.deriveFont(16f));
        messagesTabbedPane.setBackground(new Color(245, 245, 245));

        Font areaFont = BASE_FONT.deriveFont(14f);
        Color borderColor = new Color(200, 200, 200);

        JPanel successPanel = createMessageSubPanel(
                "Успех",
                config.getSuccessMessages(),
                areaFont,
                borderColor
        );

        JPanel failurePanel = createMessageSubPanel(
                "Ошибки",
                config.getFailureMessages(),
                areaFont,
                borderColor
        );

        JPanel reportPanel = createMessageSubPanel(
                "Отчеты",
                config.getReportMessages(),
                areaFont,
                borderColor
        );

        messagesTabbedPane.addTab("Успех", successPanel);
        messagesTabbedPane.addTab("Ошибки", failurePanel);
        messagesTabbedPane.addTab("Отчеты", reportPanel);

        panel.add(messagesTabbedPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createMessageSubPanel(String title, List<String> messages,
                                         Font areaFont, Color borderColor) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel(title);
        label.setFont(HEADER_FONT.deriveFont(Font.BOLD, 16f));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        panel.add(label, BorderLayout.NORTH);

        JTextArea area = new JTextArea(15, 50);
        area.setFont(areaFont);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setText(String.join("\n", messages));

        JScrollPane scrollPane = new JScrollPane(area);
        scrollPane.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(borderColor),
                        "Каждое сообщение с новой строки"
                ),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        area.setToolTipText("<html><body style='width: 300px'>"
                + "Вводите сообщения, каждое с новой строки.<br>"
                + "Пример:<br>"
                + "Операция успешно завершена!<br>"
                + "Все системы работают нормально"
                + "</body></html>");

        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel examplePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        examplePanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 5));
        JLabel exampleLabel = new JLabel("Пример: \"Операция выполнена успешно в %time%\"");
        exampleLabel.setFont(SMALLER_FONT);
        exampleLabel.setForeground(new Color(100, 100, 100));
        examplePanel.add(exampleLabel);

        panel.add(examplePanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createPathsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel header = new JLabel("Настройки путей");
        header.setFont(HEADER_FONT.deriveFont(18f));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = 0;
        panel.add(header, gbc);

        JLabel pathLabel = new JLabel("Путь к изображениям:");
        pathLabel.setFont(BASE_FONT);
        gbc.gridwidth = 1;
        gbc.gridy = 1;
        gbc.gridx = 0;
        panel.add(pathLabel, gbc);

        picsPathField = new JTextField(config.getPicsToStartPath(), 25);
        picsPathField.setFont(BASE_FONT);
        picsPathField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        ));
        gbc.gridx = 1;
        panel.add(picsPathField, gbc);

        JButton browseBtn = new JButton("Обзор...");
        browseBtn.addActionListener(e -> choosePicsDirectory()); // ВОССТАНАВЛИВАЕМ ОБРАБОТЧИК
        styleButton(browseBtn, PRIMARY_COLOR, middleGray);
        browseBtn.setPreferredSize(new Dimension(120, 35));

        gbc.gridx = 2;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(browseBtn, gbc);

        JButton openReadmeBtn = new JButton("Открыть руководство");
        openReadmeBtn.addActionListener(e -> openReadme());
        styleButton(openReadmeBtn, WARNING_COLOR, middleGray);
        openReadmeBtn.setPreferredSize(new Dimension(200, 35));
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(openReadmeBtn, gbc);

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setForeground(new Color(220, 220, 220));
        gbc.gridy = 3;
        gbc.insets = new Insets(20, 0, 10, 0);
        panel.add(separator, gbc);

        picsPathField.setToolTipText("<html>Укажите полный путь к папке с изображениями<br>Пример: C:\\Users\\User\\Pictures\\BotImages</html>");
        browseBtn.setToolTipText("Выбрать папку через проводник");
        openReadmeBtn.setToolTipText("Открыть файл с инструкциями в формате PDF");

        gbc.gridy = 4;
        gbc.weighty = 1.0;
        panel.add(Box.createGlue(), gbc);

        return panel;
    }

    private void styleButton(JButton button, Color bgColor, Color textColor) {

        button.setFont(BUTTON_FONT.deriveFont(Font.BOLD));
        button.setForeground(textColor);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);

        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Border emptyBorder = BorderFactory.createEmptyBorder(10, 25, 10, 25);
        Border lineBorder = BorderFactory.createLineBorder(bgColor.darker(), 1);
        Border compoundBorder = BorderFactory.createCompoundBorder(lineBorder, emptyBorder);
        button.setBorder(compoundBorder);

        button.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                KeyStroke.getKeyStroke("SPACE"), "none"
        );
        button.addActionListener(e -> {
            Timer pressTimer = new Timer(50, ev -> button.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(bgColor.darker().darker(), 1),
                    emptyBorder
            )));
            pressTimer.setRepeats(false);
            pressTimer.start();
        });
    }

    private void choosePicsDirectory() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            picsPathField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void openReadme() {
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
        try (FileWriter writer = new FileWriter(readmeFile)) {
            writer.write("# Руководство пользователя\n\n");
            writer.write("Основные файлы конфигурации должны находиться в папке `bot_sources`\n");
            writer.write("Полная документация доступна по [ссылке](google.com)");
        }
    }

    private JPanel createGeneralPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 20, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        attemptsSpinner = new JSpinner(new SpinnerNumberModel(
                config.getAttemptsAmount(), 1, 100, 1));

        sleepDurationSpinner = new JSpinner(new SpinnerNumberModel(
                config.getSleepDurationMinutes(), 1, 1440, 1));

        successCheck = new JCheckBox("Уведомлять об успехе", config.isSuccessNotification());
        failureCheck = new JCheckBox("Уведомлять о неудаче", config.isFailureNotification());
        reportCheck = new JCheckBox("Отправлять отчет", config.isReportNotification());
        mondayCheck = new JCheckBox("Недельная виртуалка", config.weeklySU_IsEnabled());

        addLabeledComponent(panel, "Количество попыток:", attemptsSpinner, 0, gbc);
        addLabeledComponent(panel, "Длительность сна (минут):", sleepDurationSpinner, 1, gbc);

        gbc.gridx = 0;
        gbc.gridwidth = 2;
        addCheckbox(panel, successCheck, 2, gbc);
        addCheckbox(panel, failureCheck, 3, gbc);
        addCheckbox(panel, reportCheck, 4, gbc);
        addCheckbox(panel, mondayCheck, 5, gbc);

        gbc.gridy = 7;
        gbc.weighty = 0.5;
        panel.add(Box.createGlue(), gbc);

        gbc.gridy = 8;
        panel.add(createMonitoringPanel(), gbc);

        // Пустое пространство
        gbc.gridy = 9;
        gbc.weighty = 1.0;
        panel.add(Box.createGlue(), gbc);

        panel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 10));
        return panel;
    }

    private JPanel createMonitoringPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(PRIMARY_COLOR),
                        "Прохождение виртуалки"
                ),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        monitoringStatusLabel = new JLabel();
        monitoringStatusLabel.setFont(BASE_FONT.deriveFont(Font.BOLD));
        monitoringStatusLabel.setForeground(TEXT_COLOR);
        updateMonitoringStatus();

        monitoringToggleButton = new JButton(config.isMonitoringEnabled() ? "Деактивировать" : "Активировать");
        styleButton(monitoringToggleButton,
                config.isMonitoringEnabled()
                        ? new Color(234, 67, 53) // Красный
                        : new Color(66, 133, 244), // Синий
                Color.WHITE
        );
        monitoringToggleButton.setPreferredSize(new Dimension(180, 35));
        monitoringToggleButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 1, 3, 1, new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(12, 30, 12, 30)
        ));
        monitoringToggleButton.revalidate();
        monitoringToggleButton.repaint();
        monitoringToggleButton.setContentAreaFilled(true);

        monitoringToggleButton.addActionListener(e -> {
            config.setMonitoringEnabled(!config.isMonitoringEnabled());
            updateMonitoringStatus();
            styleMonitoringButton();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.add(monitoringToggleButton);

        panel.add(monitoringStatusLabel, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private void updateMonitoringStatus() {
        boolean isActive = config.isMonitoringEnabled();
        monitoringStatusLabel.setText("Мониторинг виртуальной вселенной: " +
                (isActive ? "активен" : "неактивен"));
        monitoringToggleButton.setText(isActive ? "Деактивировать" : "Активировать");
    }

    private void styleMonitoringButton() {
        Color bgColor = config.isMonitoringEnabled()
                ? DANGER_COLOR
                : PRIMARY_COLOR;

        monitoringToggleButton.setBackground(bgColor);
        monitoringToggleButton.setForeground(middleGray);
        monitoringToggleButton.setText(config.isMonitoringEnabled() ? "Деактивировать" : "Активировать");

        monitoringToggleButton.revalidate();
        monitoringToggleButton.repaint();
    }

    private void addLabeledComponent(JPanel panel, String label, JComponent component,
                                     int yPos, GridBagConstraints gbc) {
        gbc.gridx = 0;
        gbc.gridy = yPos;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        panel.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        panel.add(component, gbc);
    }

    private void addCheckbox(JPanel panel, JCheckBox checkbox, int yPos, GridBagConstraints gbc) {
        gbc.gridy = yPos;
        panel.add(checkbox, gbc);
    }

    private void saveConfig(ActionEvent e) {
        config.setAttemptsAmount((Integer) attemptsSpinner.getValue());
        config.setSleepDurationMinutes((Integer) sleepDurationSpinner.getValue());
        config.setSuccessNotification(successCheck.isSelected());
        config.setFailureNotification(failureCheck.isSelected());
        config.setReportNotification(reportCheck.isSelected());
        config.setWeekSU_IsEnabled(mondayCheck.isSelected());

        config.setBotToken(botTokenField.getText());
        config.setChatId(chatIdField.getText());
        config.setPicsToStartPath(picsPathField.getText());

        config.setSuccessMessages(Arrays.asList(successMessagesArea.getText().split("\n")));
        config.setFailureMessages(Arrays.asList(failureMessagesArea.getText().split("\n")));
        config.setReportMessages(Arrays.asList(reportMessagesArea.getText().split("\n")));

        ConfigManager.saveConfig(config);
        showConfirmationPanel();
    }

    private Font customFont;
    private void loadCustomFont() {
        try (InputStream is = new FileInputStream(config.getPicsToStartPath() + File.separator + "font.ttf")) {
            customFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(36f);
        } catch (Exception e) {
            customFont = new Font("Meiryo", Font.BOLD, 26);
            System.err.println("Error loading custom font: " + e.getMessage());
        }
    }

    private void showConfirmationPanel() {
        getContentPane().removeAll();

        // Восстанавливаем фоновую панель
        BackgroundPanel bgPanel = (BackgroundPanel) getContentPane();
        bgPanel.removeAll();
        bgPanel.setLayout(null);

        JPanel animationPanel = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g.create();
                g2d.setComposite(AlphaComposite.SrcOver.derive(0.7f));
                g2d.setColor(new Color(0, 0, 0, 100));
                g2d.fillRect(0, 0, getWidth(), getHeight());
                g2d.dispose();
            }
        };
        animationPanel.setOpaque(false);
        animationPanel.setBounds(0, 0, getWidth(), getHeight());
        bgPanel.add(animationPanel);
        revalidate();
        repaint();

        Point startPoint;
        startPoint = Objects.requireNonNullElseGet(lastSaveClickPoint, () -> new Point(getWidth() / 2, getHeight() - 50));

        Font largeFont = customFont != null ?
                customFont.deriveFont(36f) :
                new Font("Meiryo", Font.BOLD, 36);

        JLabel animatedLabel = new JLabel("セーブ完了");
        animatedLabel.setFont(largeFont);
        animatedLabel.setSize(animatedLabel.getPreferredSize());

        int startY = getHeight() - animatedLabel.getHeight() - 60;
        animatedLabel.setLocation(
                startPoint.x - animatedLabel.getWidth() / 2,
                startY
        );
        animationPanel.add(animatedLabel);

        final int[] time = {0};
        final int animationDuration = 24; // 50 кадров * 20 мс = 1000 мс (1 секунда)

        Timer timer = new Timer(20, e -> {
            time[0]++;

            float hue = (time[0] * 3f) % animationDuration / animationDuration;
            Color color = Color.getHSBColor(hue, 0.95f, 1.0f);

            // Плавное затухание в конце анимации
            float alpha = 1.0f;
            if (time[0] > animationDuration * 0.7) {
                alpha = 1.0f - (time[0] - animationDuration * 0.7f) / (animationDuration * 0.3f);
                alpha = Math.max(0, Math.min(1, alpha));
            }

            animatedLabel.setForeground(new Color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    (int)(alpha * 255)
            ));

            // Завершение анимации через 1 секунду
            if (time[0] >= animationDuration) {
                ((Timer) e.getSource()).stop();
                dispose();
            }

            repaint();
        });
        timer.start();
    }


    static class BackgroundPanel extends JPanel {
        private final Image backgroundImage;
        private final float transparency;
        private Color edgeColor = Color.WHITE;

        public BackgroundPanel(Image backgroundImage, float transparency) {
            this.backgroundImage = backgroundImage;
            this.transparency = transparency;
            setLayout(new BorderLayout());
            calculateEdgeColor();
        }

        private void calculateEdgeColor() {
            try {
                BufferedImage bufferedImage = (BufferedImage) backgroundImage;
                int rgb = bufferedImage.getRGB(0, 0);
                edgeColor = new Color(rgb);
            } catch (Exception e) {
                edgeColor = new Color(40, 40, 40); // Fallback цвет
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g.create();

            g2d.setColor(edgeColor);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            if (backgroundImage != null) {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency));

                double widthRatio = (double) getWidth() / backgroundImage.getWidth(null);
                double heightRatio = (double) getHeight() / backgroundImage.getHeight(null);
                double scale = Math.max(widthRatio, heightRatio);

                int scaledWidth = (int) (backgroundImage.getWidth(null) * scale);
                int scaledHeight = (int) (backgroundImage.getHeight(null) * scale);

                int x = (getWidth() - scaledWidth) / 2;
                int y = (getHeight() - scaledHeight) / 2;

                g2d.drawImage(backgroundImage, x, y, scaledWidth, scaledHeight, null);
            }
            g2d.dispose();
        }
    }
}