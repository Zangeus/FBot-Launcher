package Config.ConfigWindowStructure;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class StyleManager {

    public static final Color PRIMARY_COLOR = new Color(70, 130, 180);
    public static final Color DANGER_COLOR = new Color(220, 80, 80);
    public static final Color WARNING_COLOR = new Color(255, 165, 0);
    public static final Color SUCCESS_COLOR = new Color(15, 157, 88);
    public static final Color SECONDARY_COLOR = new Color(66, 133, 244);
    public static final Color TEXT_COLOR = new Color(50, 50, 50);
    public static final Color BORDER_COLOR = new Color(200, 200, 200);

    public static final Font BASE_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font HEADER_FONT = new Font("Segoe UI SemiBold", Font.BOLD, 16);
    public static final Font TITLE_FONT = new Font("Segoe UI SemiBold", Font.BOLD, 18);
    public static final Font BUTTON_FONT = new Font("Segoe UI SemiBold", Font.BOLD, 14);
    public static final Font SMALLER_FONT = new Font("Segoe UI", Font.PLAIN, 12);

    public static void styleButton(JButton button, Color bgColor, Color textColor) {
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

    public static Border createTextFieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                BorderFactory.createEmptyBorder(5, 8, 5, 8)
        );
    }

    public static Border createTitledBorder(String title) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR),
                        title
                ),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)
        );
    }

    public static void setupUIManager() {
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
    }
}