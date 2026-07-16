package com.empanak.dmojutils.common;

import lombok.Setter;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class ColorRowRenderer extends DefaultTableCellRenderer {
    private Color[] columnColors;
    private JCheckBox checkBox = new JCheckBox();
    @Setter
    private int negativeOffset = 0;
    public ColorRowRenderer(Color[] colors) {
        this.columnColors = colors;
        this.checkBox.setHorizontalAlignment(JCheckBox.CENTER);
        this.checkBox.setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        int colorIndex = column - negativeOffset;
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        c.setFont(table.getFont().deriveFont(Font.BOLD));
        if (column < negativeOffset) {
            c.setBackground(row == 0 ? Color.BLACK : Color.WHITE);
            return c;
        }

        if(row == 0){
            c = super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
            c.setBackground(columnColors[colorIndex]);
            return c;
        }
        if(value != null && ("AC".equals(value.toString().trim()) || "X".equals(value.toString().trim()))){
            c.setBackground(getDarkerColor(columnColors[colorIndex], 0.9f));
            return c;
        }
        if(value != null && ("FtS".equals(value.toString().trim()) || "XFtS".equals(value.toString().trim()))){
            c.setBackground(Color.decode("#C7BD67"));
            return c;
        }
        c.setBackground(Color.WHITE);
        return c;
    }

    private Color getDarkerColor(Color color, float brightnessFactor) {
        if(color == null) return Color.WHITE;
        float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float newBrightness = hsb[2] * brightnessFactor;
        newBrightness = Math.max(0.0f, newBrightness);

        return Color.getHSBColor(hsb[0], hsb[1], newBrightness);
    }
}
