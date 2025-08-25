package Config.ConfigWindowStructure;

import Config.LauncherConfig;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class ButtonPanel extends JPanel {
    private final ConfigWindow parent;
    private Point lastSaveClickPoint;

    public ButtonPanel(ConfigWindow parent) {
        this.parent = parent;
        initUI();
    }

    private void initUI() {
        setLayout(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        setOpaque(true);

        JButton saveBtn = new JButton("Сохранить");
        StyleManager.styleButton(saveBtn, StyleManager.SUCCESS_COLOR, Color.WHITE);

        saveBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                lastSaveClickPoint = e.getPoint();
                lastSaveClickPoint = SwingUtilities.convertPoint(saveBtn, lastSaveClickPoint, parent);
            }
        });

        saveBtn.addActionListener(this::saveConfig);

        JButton cancelBtn = new JButton("Отмена");
        StyleManager.styleButton(cancelBtn, StyleManager.SECONDARY_COLOR, Color.WHITE);
        cancelBtn.addActionListener(e -> parent.dispose());

        add(saveBtn);
        add(cancelBtn);
    }

    private void saveConfig(ActionEvent e) {
        parent.saveConfig(); // Без проверки результата
        parent.showConfirmationPanel(lastSaveClickPoint);
    }
}