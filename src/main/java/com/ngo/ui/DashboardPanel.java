package com.ngo.ui;

import com.ngo.dao.DashboardDao;
import com.ngo.util.ErrorHandler;
import java.awt.*;
import java.util.Map;
import javax.swing.*;

/** Summary cards; every number is computed by SQL aggregate queries. */
public class DashboardPanel extends JPanel implements Refreshable {
    private final JPanel cards = new JPanel(new GridLayout(0, 5, 12, 12));

    public DashboardPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JLabel t = new JLabel("NGO Dashboard");
        t.setFont(t.getFont().deriveFont(Font.BOLD, 22f)); t.setForeground(UiUtil.DARK);
        add(t, BorderLayout.NORTH);
        add(cards, BorderLayout.CENTER);
        JButton r = new JButton("Refresh");
        r.addActionListener(e -> refresh());
        JPanel s = new JPanel(new FlowLayout(FlowLayout.LEFT)); s.add(r);
        add(s, BorderLayout.SOUTH);
    }

    @Override public void refresh() {
        cards.removeAll();
        try {
            for (Map.Entry<String, String> e : DashboardDao.totals().entrySet()) {
                JPanel c = new JPanel(new BorderLayout());
                c.setBackground(UiUtil.LIGHT);
                c.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(UiUtil.GREEN, 1), BorderFactory.createEmptyBorder(14, 12, 14, 12)));
                JLabel v = new JLabel(e.getValue(), SwingConstants.CENTER);
                v.setFont(v.getFont().deriveFont(Font.BOLD, 24f)); v.setForeground(UiUtil.GREEN);
                JLabel k = new JLabel(e.getKey(), SwingConstants.CENTER);
                c.add(v, BorderLayout.CENTER); c.add(k, BorderLayout.SOUTH);
                cards.add(c);
            }
        } catch (Exception e) {
            cards.add(new JLabel(ErrorHandler.message(e)));
        }
        cards.revalidate(); cards.repaint();
    }

    public int cardCount() { return cards.getComponentCount(); }
}
