package com.ngo.ui;

import com.ngo.dao.ReportDao;
import com.ngo.dao.ResultTable;
import com.ngo.util.ErrorHandler;
import java.awt.*;
import javax.swing.*;

/** Runs the selected report query against MySQL and shows the result in a JTable. */
public class ReportsPanel extends JPanel implements Refreshable {
    private final JComboBox<String> chooser = new JComboBox<>(ReportDao.REPORTS.keySet().toArray(new String[0]));
    private final JTable table = UiUtil.table();
    private final JLabel info = new JLabel(" ");

    public ReportsPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel t = new JLabel("Reports   ");
        t.setFont(t.getFont().deriveFont(Font.BOLD, 18f)); t.setForeground(UiUtil.DARK);
        JButton run = new JButton("Run Report");
        run.addActionListener(e -> run());
        chooser.addActionListener(e -> run());
        top.add(t); top.add(chooser); top.add(run); top.add(info);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
    }

    @Override public void refresh() { run(); }

    public int run() {
        try {
            ResultTable r = ReportDao.run((String) chooser.getSelectedItem());
            UiUtil.apply(table, UiUtil.model(r));
            info.setText(r.rows.size() + " rows (live SQL query)");
            return r.rows.size();
        } catch (Exception e) {
            info.setText(ErrorHandler.message(e));
            UiUtil.error(this, ErrorHandler.message(e));
            return -1;
        }
    }

    public void select(String name) { chooser.setSelectedItem(name); }
    public int rows() { return table.getRowCount(); }
}
