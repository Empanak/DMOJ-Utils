package com.empanak.dmojutils.common;

import lombok.Getter;
import lombok.NonNull;

import javax.swing.*;
import java.awt.*;

public class JTextFieldPlaceholder extends JTextField {
    @Getter
    private String placeholder;

    public JTextFieldPlaceholder(@NonNull String placeholder) {
        this.placeholder = placeholder;
    }

    @Override
    protected void paintComponent(Graphics g){
        super.paintComponent(g);
        if(getText().isEmpty() && placeholder != null){
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Color.GRAY);
            g2.setFont(getFont().deriveFont(Font.ITALIC));

            Insets insets = getInsets();
            FontMetrics fontM = g2.getFontMetrics();
            int x = insets.left;
            int backgroundHeight = getHeight() - insets.top - insets.bottom;
            int textY = insets.top + (backgroundHeight - fontM.getHeight()) / 2 + fontM.getAscent();
            g2.drawString(placeholder, insets.left + 5, textY);
        }
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }
}
