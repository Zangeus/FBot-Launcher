package Config.ConfigWindowStructure;

import Config.ConfigManager;
import Config.LauncherConfig;
import Waiters.Main;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Objects;

public class ConfigWindow extends JFrame {
    private final LauncherConfig config;
    private JTabbedPane tabbedPane;
    private Point lastSaveClickPoint;
    private Font customFont;

    private static final int MIN_WIDTH = 800;
    private static final int MAX_WIDTH = 950;
    private static final int PREF_HEIGHT = 600;

    public ConfigWindow() {
        config = ConfigManager.loadConfig();
        loadCustomFont();
        initUI();
    }

    private void initUI() {
        // применяем тему из конфига ДО setupUIManager
        StyleManager.setDarkTheme(config.isDarkThemeEnabled());
        StyleManager.setupUIManager();

        setTitle("Настройки приложения");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setUndecorated(true);
        setLocationRelativeTo(null);

        try {
            ImageIcon icon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/icon.png")));
            if (icon.getImage() != null) {
                setIconImage(icon.getImage());
            }
        } catch (Exception e) {
            System.err.println("Не удалось загрузить иконку: " + e.getMessage());
        }

        Image backgroundImage = null;
        try {
            ImageIcon bgIcon = new ImageIcon(new File(
                    config.getPicsToStartPath() + File.separator + "background.png").getAbsolutePath());
            backgroundImage = bgIcon.getImage();
        } catch (Exception e) {
            System.err.println("Не удалось загрузить фоновое изображение: " + e.getMessage());
        }

        int calculatedWidth = Math.max(MIN_WIDTH, 900);

        setSize(calculatedWidth, PREF_HEIGHT);
        setPreferredSize(new Dimension(calculatedWidth, PREF_HEIGHT));
        setMinimumSize(new Dimension(MIN_WIDTH, PREF_HEIGHT));
        setMaximumSize(new Dimension(MAX_WIDTH, PREF_HEIGHT));
        setResizable(false);
        setLocationRelativeTo(null);

        BackgroundPanel bgPanel = new BackgroundPanel(backgroundImage, 0.75f);
        bgPanel.setOpaque(true);
        bgPanel.setWindow(this);
        setContentPane(bgPanel);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (!Main.isRunning()) {
                    System.exit(0);
                }
            }
        });

        addCloseButton(bgPanel);
        initComponents();
    }

    private void addCloseButton(BackgroundPanel bgPanel) {
        JButton closeButton = new JButton("×");
        closeButton.setFont(new Font("Arial", Font.BOLD, 20));
        closeButton.setForeground(new Color(100, 100, 100));
        closeButton.setBackground(new Color(0, 0, 0, 0));
        closeButton.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        closeButton.setFocusPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        closeButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                closeButton.setForeground(new Color(220, 80, 80));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                closeButton.setForeground(new Color(100, 100, 100));
            }
        });

        closeButton.addActionListener(e -> dispose());

        bgPanel.setLayout(new BorderLayout());
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topPanel.setOpaque(false);
        topPanel.add(closeButton);
        bgPanel.add(topPanel, BorderLayout.NORTH);
    }

    private void initComponents() {

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(StyleManager.HEADER_FONT);

        JPanel generalPanel = new GeneralPanel(config, this); // Передаем ссылку на окно
        JPanel telegramPanel = new TelegramPanel(config);
        JPanel pathsPanel = new PathsPanel(config);
        JPanel messagesPanel = new MessagesPanel(config);

        tabbedPane.addTab("Основные", generalPanel);
        tabbedPane.addTab("Telegram", telegramPanel);
        tabbedPane.addTab("Пути", pathsPanel);
        tabbedPane.addTab("Сообщения", messagesPanel);

        tabbedPane.setOpaque(false);

        ButtonPanel buttonPanel = new ButtonPanel(this);
        BackgroundPanel bgPanel = (BackgroundPanel) getContentPane();

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setOpaque(false);
        centerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        centerPanel.add(tabbedPane, BorderLayout.CENTER);

        bgPanel.add(centerPanel, BorderLayout.CENTER);
        bgPanel.add(buttonPanel, BorderLayout.SOUTH);
    }

    private void loadCustomFont() {
        try {
            File fontFile = new File(config.getPicsToStartPath() + File.separator + "font.ttf");
            if (fontFile.exists()) {
                java.io.InputStream is = new java.io.FileInputStream(fontFile);
                customFont = Font.createFont(Font.TRUETYPE_FONT, is).deriveFont(36f);
            } else {
                customFont = new Font("Meiryo", Font.BOLD, 26);
                System.err.println("Файл шрифта не найден, используется стандартный шрифт");
            }
        } catch (Exception e) {
            customFont = new Font("Meiryo", Font.BOLD, 26);
            System.err.println("Ошибка загрузки пользовательского шрифта: " + e.getMessage());
        }
    }

    public void saveConfig() {
        GeneralPanel generalPanel = (GeneralPanel) tabbedPane.getComponentAt(0);
        TelegramPanel telegramPanel = (TelegramPanel) tabbedPane.getComponentAt(1);
        PathsPanel pathsPanel = (PathsPanel) tabbedPane.getComponentAt(2);
        MessagesPanel messagesPanel = (MessagesPanel) tabbedPane.getComponentAt(3);

        config.setAttemptsAmount(generalPanel.getAttemptsAmount());
        config.setSleepDurationMinutes(generalPanel.getSleepDuration());
        config.setSuccessNotification(generalPanel.isSuccessNotificationEnabled());
        config.setFailureNotification(generalPanel.isFailureNotificationEnabled());
        config.setReportNotification(generalPanel.isReportNotificationEnabled());
        config.setDarkThemeEnabled(generalPanel.isDarkThemeEnabled());
        config.setSU_Monitoring(generalPanel.isMonitoringEnabled());

        config.setBotToken(telegramPanel.getBotToken());
        config.setChatId(telegramPanel.getChatId());
        config.setPicsToStartPath(pathsPanel.getPicsPath());
        config.setMuMuPlayerPath(pathsPanel.getMuMuPlayerPath());
        config.setStarRailCopilotPath(pathsPanel.getStarRailCopilot());

        config.setSuccessMessages(messagesPanel.getSuccessMessages());
        config.setFailureMessages(messagesPanel.getFailureMessages());
        config.setReportMessages(messagesPanel.getReportMessages());

        ConfigManager.saveConfig(config);
    }

    public void showConfirmationPanel(Point clickPoint) {
        this.lastSaveClickPoint = clickPoint;

        BackgroundPanel bgPanel = (BackgroundPanel) getContentPane();
        bgPanel.removeAll();
        bgPanel.setLayout(null);

        JPanel animationPanel = createAnimationPanel();
        bgPanel.add(animationPanel);
        revalidate();
        repaint();

        JLabel animatedLabel = createAnimatedLabel();
        animationPanel.add(animatedLabel);

        Timer timer = createAnimationTimer(animatedLabel);
        timer.start();
    }

    private JPanel createAnimationPanel() {
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
        return animationPanel;
    }

    private JLabel createAnimatedLabel() {
        Point startPoint = Objects.requireNonNullElseGet(
                lastSaveClickPoint,
                () -> new Point(getWidth() / 2, getHeight() - 50)
        );

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
        return animatedLabel;
    }

    private Timer createAnimationTimer(JLabel animatedLabel) {
        final int[] time = {0};
        final int animationDuration = 24;

        return new Timer(20, e -> {
            time[0]++;

            float hue = (time[0] * 3f) % animationDuration / animationDuration;
            Color color = Color.getHSBColor(hue, 0.95f, 1.0f);

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

            if (time[0] >= animationDuration) {
                ((Timer) e.getSource()).stop();
                dispose();
            }

            repaint();
        });
    }

    // Метод для обновления темы всего окна
    public void updateTheme() {
        StyleManager.setupUIManager();
        SwingUtilities.updateComponentTreeUI(this);
        repaint();
    }

    static class GeneralPanel extends JPanel {
        private final LauncherConfig config;
        private final ConfigWindow parentWindow; // Ссылка на родительское окно
        private JSpinner attemptsSpinner;
        private JSpinner sleepDurationSpinner;
        private JCheckBox successCheck;
        private JCheckBox failureCheck;
        private JCheckBox reportCheck;
        private JLabel monitoringStatusLabel;
        private JButton monitoringToggleButton;

        public GeneralPanel(LauncherConfig config, ConfigWindow parentWindow) {
            this.config = config;
            this.parentWindow = parentWindow;
            initUI();
            loadConfigData();
        }

        private void initUI() {

            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 20, 5, 5);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.weightx = 1.0;
            gbc.weighty = 0;

            JLabel header = new JLabel("Основные настройки");
            header.setFont(StyleManager.HEADER_FONT.deriveFont(18f));
            header.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
            gbc.gridwidth = 2;
            gbc.gridx = 0;
            gbc.gridy = 0;
            add(header, gbc);

            gbc.gridwidth = 1;

            JLabel attemptsLabel = new JLabel("Количество попыток:");
            attemptsLabel.setFont(StyleManager.BASE_FONT);
            gbc.gridy = 1;
            gbc.gridx = 0;
            gbc.weightx = 0; // Метка не растягивается
            add(attemptsLabel, gbc);

            attemptsSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
            attemptsSpinner.setFont(StyleManager.BASE_FONT);
            gbc.gridx = 1;
            gbc.weightx = 1.0; // Поле ввода растягивается
            add(attemptsSpinner, gbc);

            JLabel sleepLabel = new JLabel("Длительность сна (минут):");
            sleepLabel.setFont(StyleManager.BASE_FONT);
            gbc.gridx = 0;
            gbc.gridy = 2;
            gbc.weightx = 0;
            add(sleepLabel, gbc);

            sleepDurationSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1440, 1));
            sleepDurationSpinner.setFont(StyleManager.BASE_FONT);
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            add(sleepDurationSpinner, gbc);

            gbc.gridwidth = 2;
            gbc.weightx = 1.0;

            successCheck = new JCheckBox("Уведомлять об успехе");
            successCheck.setFont(StyleManager.BASE_FONT);
            gbc.gridx = 0;
            gbc.gridy = 3;
            add(successCheck, gbc);

            failureCheck = new JCheckBox("Уведомлять о неудаче");
            failureCheck.setFont(StyleManager.BASE_FONT);
            gbc.gridy = 4;
            add(failureCheck, gbc);

            reportCheck = new JCheckBox("Отправлять отчет");
            reportCheck.setFont(StyleManager.BASE_FONT);
            gbc.gridy = 5;
            add(reportCheck, gbc);

            // Убираем чекбокс темной темы

            // Панель мониторинга - тоже полная ширина
            gbc.gridy = 6;
            gbc.insets = new Insets(20, 0, 10, 0);
            add(createMonitoringPanel(), gbc);

            // Пустое пространство внизу для прижатия всего к верху
            gbc.gridy = 7;
            gbc.weighty = 1.0; // Занимает все оставшееся пространство
            add(Box.createGlue(), gbc);
        }

        private JPanel createMonitoringPanel() {
            JPanel panel = new JPanel(new BorderLayout());
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(
                            BorderFactory.createLineBorder(StyleManager.PRIMARY_COLOR),
                            "Прохождение виртуалки"
                    ),
                    BorderFactory.createEmptyBorder(10, 15, 10, 15)
            ));

            monitoringStatusLabel = new JLabel();
            monitoringStatusLabel.setFont(StyleManager.BASE_FONT.deriveFont(Font.BOLD));

            // 🔒 Даем FlatLaf самому красить надпись
            monitoringStatusLabel.putClientProperty("FlatLaf.styleClass", "default");

            updateMonitoringStatus();

            monitoringToggleButton = new JButton(config.isSU_Monitoring() ? "Деактивировать" : "Активировать");
            StyleManager.styleButton(monitoringToggleButton, StyleManager.PRIMARY_COLOR, Color.WHITE);
            monitoringToggleButton.setPreferredSize(new Dimension(180, 35));
            monitoringToggleButton.addActionListener(e -> toggleMonitoring());

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
            buttonPanel.add(monitoringToggleButton);

            panel.add(monitoringStatusLabel, BorderLayout.CENTER);
            panel.add(buttonPanel, BorderLayout.SOUTH);

            return panel;
        }

        private void updateMonitoringStatus() {
            boolean isActive = config.isSU_Monitoring();
            monitoringStatusLabel.setText("Мониторинг виртуальной вселенной: " +
                    (isActive ? "активен" : "неактивен"));
        }

        private void toggleMonitoring() {
            // Переключаем мониторинг
            config.setSU_Monitoring(!config.isSU_Monitoring());

            // Переключаем тему (инвертируем текущую)
            boolean newTheme = !config.isDarkThemeEnabled();
            config.setDarkThemeEnabled(newTheme);
            StyleManager.setDarkTheme(newTheme);

            // Сохраняем конфиг
            ConfigManager.saveConfig(config);

            // Обновляем интерфейс
            updateMonitoringStatus();
            updateMonitoringButtonStyle();

            // Обновляем тему всего окна
            parentWindow.updateTheme();
        }

        private void updateMonitoringButtonStyle() {
            // Кнопка всегда остается синей, меняется только текст
            monitoringToggleButton.setText(config.isSU_Monitoring() ? "Деактивировать" : "Активировать");
        }

        private void loadConfigData() {
            attemptsSpinner.setValue(config.getAttemptsAmount());
            sleepDurationSpinner.setValue(config.getSleepDurationMinutes());
            successCheck.setSelected(config.isSuccessNotification());
            failureCheck.setSelected(config.isFailureNotification());
            reportCheck.setSelected(config.isReportNotification());
            updateMonitoringStatus();
            updateMonitoringButtonStyle();
        }

        public int getAttemptsAmount() {
            return (Integer) attemptsSpinner.getValue();
        }

        public int getSleepDuration() {
            return (Integer) sleepDurationSpinner.getValue();
        }

        public boolean isSuccessNotificationEnabled() {
            return successCheck.isSelected();
        }

        public boolean isFailureNotificationEnabled() {
            return failureCheck.isSelected();
        }

        public boolean isReportNotificationEnabled() {
            return reportCheck.isSelected();
        }

        public boolean isDarkThemeEnabled() {
            return config.isDarkThemeEnabled();
        }

        public boolean isMonitoringEnabled() {
            return config.isSU_Monitoring();
        }
    }
}