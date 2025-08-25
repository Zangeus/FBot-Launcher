package Config.ConfigWindowStructure;

import Config.LauncherConfig;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MessagesPanel extends JPanel {
    private final LauncherConfig config;
    private JTextArea successMessagesArea;
    private JTextArea failureMessagesArea;
    private JTextArea reportMessagesArea;

    public MessagesPanel(LauncherConfig config) {
        this.config = config;
        initUI();
        loadConfigData();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        JTabbedPane messagesTabbedPane = new JTabbedPane();
        messagesTabbedPane.setFont(StyleManager.HEADER_FONT.deriveFont(16f));

        // Панель для сообщений об успехе
        successMessagesArea = new JTextArea();
        JPanel successPanel = createMessageSubPanel("Успех", successMessagesArea);

        // Панель для сообщений об ошибках
        failureMessagesArea = new JTextArea();
        JPanel failurePanel = createMessageSubPanel("Ошибки", failureMessagesArea);

        // Панель для отчетов
        reportMessagesArea = new JTextArea();
        JPanel reportPanel = createMessageSubPanel("Отчеты", reportMessagesArea);

        messagesTabbedPane.addTab("Успех", successPanel);
        messagesTabbedPane.addTab("Ошибки", failurePanel);
        messagesTabbedPane.addTab("Отчеты", reportPanel);

        add(messagesTabbedPane, BorderLayout.CENTER);
    }

    private JPanel createMessageSubPanel(String title, JTextArea textArea) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel label = new JLabel(title);
        label.setFont(StyleManager.HEADER_FONT.deriveFont(Font.BOLD, 16f));
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        panel.add(label, BorderLayout.NORTH);

        textArea.setFont(getJapaneseFont());
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setBorder(StyleManager.createTitledBorder("Каждое сообщение с новой строки"));
        panel.add(scrollPane, BorderLayout.CENTER);

        JPanel examplePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        examplePanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 5));
        JLabel exampleLabel = new JLabel("Пример: \"Операция выполнена успешно в %time%\"");
        exampleLabel.setFont(StyleManager.SMALLER_FONT);
        exampleLabel.setForeground(new Color(100, 100, 100));
        examplePanel.add(exampleLabel);

        panel.add(examplePanel, BorderLayout.SOUTH);

        return panel;
    }

    private Font getJapaneseFont() {
        String[] jpFonts = {"Meiryo", "MS PGothic", "MS PMincho", "MS Gothic", "MS Mincho", "Yu Gothic"};
        for (String fontName : jpFonts) {
            Font font = new Font(fontName, Font.PLAIN, 14);
            if (font.getFamily().equals(fontName)) {
                return font;
            }
        }
        return new Font(Font.SANS_SERIF, Font.PLAIN, 14);
    }

    private void loadConfigData() {
        successMessagesArea.setText(String.join("\n", config.getSuccessMessages()));
        failureMessagesArea.setText(String.join("\n", config.getFailureMessages()));
        reportMessagesArea.setText(String.join("\n", config.getReportMessages()));
    }

    public List<String> getSuccessMessages() {
        return new ArrayList<>(Arrays.asList(successMessagesArea.getText().split("\\R")));
    }

    public List<String> getFailureMessages() {
        return new ArrayList<>(Arrays.asList(failureMessagesArea.getText().split("\\R")));
    }

    public List<String> getReportMessages() {
        return new ArrayList<>(Arrays.asList(reportMessagesArea.getText().split("\\R")));
    }
}