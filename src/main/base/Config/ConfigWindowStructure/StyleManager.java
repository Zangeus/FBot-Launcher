package Config.ConfigWindowStructure;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import lombok.Getter;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;

public class StyleManager {

    @Getter
    private static boolean darkTheme = true; // по умолчанию тёмная

    public static final Color PRIMARY_COLOR   = new Color(90, 156, 248);
    public static final Color DANGER_COLOR    = new Color(220, 80, 80);
    public static final Color WARNING_COLOR   = new Color(255, 193, 7);
    public static final Color SUCCESS_COLOR   = new Color(76, 175, 80);
    public static final Color SECONDARY_COLOR = new Color(66, 133, 244);
    public static final Color TEXT_COLOR      = new Color(230, 230, 230);
    public static final Color BORDER_COLOR    = new Color(80, 80, 80);

    public static final Font BASE_FONT     = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font HEADER_FONT   = new Font("Segoe UI Semibold", Font.BOLD, 16);
    public static final Font TITLE_FONT    = new Font("Segoe UI Semibold", Font.BOLD, 18);
    public static final Font BUTTON_FONT   = new Font("Segoe UI Semibold", Font.BOLD, 14);
    public static final Font SMALLER_FONT  = new Font("Segoe UI", Font.PLAIN, 12);

    public static void styleButton(JButton button, Color bgColor, Color textColor) {
        button.setFont(BUTTON_FONT);
        button.setForeground(textColor);
        button.setBackground(bgColor);
        button.setFocusPainted(false);
        button.setOpaque(true);
        button.setBorderPainted(false);

        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        Border emptyBorder = BorderFactory.createEmptyBorder(10, 25, 10, 25);
        Border lineBorder = BorderFactory.createLineBorder(bgColor.darker(), 1);
        button.setBorder(BorderFactory.createCompoundBorder(lineBorder, emptyBorder));

        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor.brighter());
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(bgColor);
            }
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
            if (darkTheme) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
            }

            UIManager.put("Button.arc", 12);
            UIManager.put("Component.arc", 12);
            UIManager.put("TextComponent.arc", 10);

            UIManager.put("TabbedPane.showTabSeparators", true);
            UIManager.put("TabbedPane.tabsUnderline", true);
            UIManager.put("TabbedPane.selectedBackground", PRIMARY_COLOR);

            UIManager.put("Button.font", BUTTON_FONT);
            UIManager.put("Label.font", BASE_FONT);
            UIManager.put("TextField.font", BASE_FONT);
            UIManager.put("TextArea.font", BASE_FONT);
            UIManager.put("Spinner.font", BASE_FONT);
            UIManager.put("CheckBox.font", BASE_FONT);
            UIManager.put("TabbedPane.font", TITLE_FONT);

        } catch (Exception e) {
            System.err.println("⚠ Не удалось применить FlatLaf: " + e.getMessage());
        }
    }

    public static void setDarkTheme(boolean dark) {
        darkTheme = dark;
        setupUIManager();
        SwingUtilities.updateComponentTreeUI(JFrame.getFrames()[0]); // обновляем окно
    }

}
