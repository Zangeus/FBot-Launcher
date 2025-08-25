package Config.ConfigWindowStructure;

import javax.swing.*;
import java.awt.*;

public class PanelHelper {
    public static void addHeader(JPanel panel, String title, GridBagConstraints gbc, int gridy) {
        JLabel header = new JLabel(title);
        header.setFont(StyleManager.HEADER_FONT.deriveFont(18f));
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 15, 0));
        gbc.gridwidth = 2;
        gbc.gridx = 0;
        gbc.gridy = gridy;
        panel.add(header, gbc);
        gbc.gridwidth = 1;
    }

    public static void addLabeledTextField(JPanel panel, String labelText, JTextField textField,
                                           GridBagConstraints gbc, int gridy) {
        JLabel label = new JLabel(labelText);
        label.setFont(StyleManager.BASE_FONT);
        gbc.gridy = gridy;
        gbc.gridx = 0;
        panel.add(label, gbc);

        textField.setFont(StyleManager.BASE_FONT);
        textField.setBorder(StyleManager.createTextFieldBorder());
        gbc.gridx = 1;
        panel.add(textField, gbc);
        gbc.gridx = 0; // Reset for next components
    }
}