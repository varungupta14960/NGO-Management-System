package com.ngo.ui;

import com.ngo.dao.ResultTable;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** Small Swing helpers shared by all panels. */
public final class UiUtil {
    public static final Color GREEN = new Color(0x1B6B4A);
    public static final Color DARK = new Color(0x0F3D2A);
    public static final Color LIGHT = new Color(0xE8F5EE);

    private UiUtil() { }

    public static DefaultTableModel model(ResultTable t) {
        DefaultTableModel m = new DefaultTableModel(t.columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        for (Object[] row : t.rows) m.addRow(row);
        return m;
    }

    public static JTable table() {
        JTable t = new JTable();
        t.setRowHeight(24);
        t.setAutoCreateRowSorter(true);
        t.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        t.getTableHeader().setFont(t.getTableHeader().getFont().deriveFont(Font.BOLD));
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        return t;
    }

    /** Applies a model and sizes each column to its content (capped), so wide text stays readable. */
    public static void apply(JTable t, DefaultTableModel m) {
        t.setModel(m);
        java.awt.FontMetrics fm = t.getFontMetrics(t.getFont());
        for (int c = 0; c < m.getColumnCount(); c++) {
            int w = fm.stringWidth(String.valueOf(m.getColumnName(c))) + 30;
            for (int r = 0; r < Math.min(m.getRowCount(), 60); r++)
                w = Math.max(w, fm.stringWidth(String.valueOf(m.getValueAt(r, c))) + 20);
            t.getColumnModel().getColumn(c).setPreferredWidth(Math.min(w, 380));
        }
    }

    public static void error(JComponent parent, String msg) {
        if (!GraphicsEnvironment.isHeadless()) JOptionPane.showMessageDialog(parent, msg, "Cannot complete action", JOptionPane.ERROR_MESSAGE);
    }

    public static void info(JComponent parent, String msg) {
        if (!GraphicsEnvironment.isHeadless()) JOptionPane.showMessageDialog(parent, msg, "Done", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(JComponent parent, String msg) {
        if (GraphicsEnvironment.isHeadless()) return true;
        return JOptionPane.showConfirmDialog(parent, msg, "Please confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }
}
