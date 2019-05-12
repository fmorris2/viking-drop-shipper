package main.org.vikingsoftware.dropshipper.listing.tool.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.image.BufferedImage;

import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.border.LineBorder;

class IconCellRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;

    @Override
    public Component getListCellRendererComponent(
            final JList<?> list,
            final Object value,
            final int index,
            final boolean isSelected,
            final boolean cellHasFocus) {
        final Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (c instanceof JLabel && value instanceof BufferedImage) {
            final JLabel l = (JLabel)c;
            l.setBorder(new LineBorder(Color.BLACK));
            l.setText("");
            final BufferedImage img = (BufferedImage)value;
            l.setIcon(new ImageIcon(img));
        }
        return c;
    }
}