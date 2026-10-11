package com.ngo.ui;

import com.ngo.service.EntityRegistry;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.swing.*;

/** Main window: navigation list on the left, the selected module on the right. */
public class MainFrame extends JFrame {
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);
    private final Map<String, JComponent> modules = new LinkedHashMap<>();

    public MainFrame() {
        super("NGO Donation, Beneficiary and Volunteer Management System");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1280, 780);
        setLocationRelativeTo(null);

        modules.put("Dashboard", new DashboardPanel());
        modules.put("Programmes", new CrudPanel(EntityRegistry.programme()));
        modules.put("Campaigns", new CrudPanel(EntityRegistry.campaign()));
        modules.put("Donors", new CrudPanel(EntityRegistry.donor()));
        modules.put("Donations", new CrudPanel(EntityRegistry.donation()));
        modules.put("Beneficiaries", new CrudPanel(EntityRegistry.beneficiary()));
        modules.put("Requests", new CrudPanel(EntityRegistry.request()));
        modules.put("Approvals", new CrudPanel(EntityRegistry.approval()));
        modules.put("Distributions", new CrudPanel(EntityRegistry.distribution()));
        modules.put("Assistance Workflow", new WorkflowPanel());
        modules.put("Volunteers", new CrudPanel(EntityRegistry.volunteer()));
        modules.put("Skills", new CrudPanel(EntityRegistry.skill()));
        modules.put("Assignments", new CrudPanel(EntityRegistry.assignment()));
        modules.put("Expenses", new CrudPanel(EntityRegistry.expense()));
        modules.put("Impact Indicators", new CrudPanel(EntityRegistry.impact()));
        modules.put("Reports", new ReportsPanel());
        for (Map.Entry<String, JComponent> e : modules.entrySet()) content.add(e.getValue(), e.getKey());

        JList<String> nav = new JList<>(modules.keySet().toArray(new String[0]));
        nav.setFont(nav.getFont().deriveFont(14f));
        nav.setFixedCellHeight(32);
        nav.setSelectionBackground(UiUtil.GREEN);
        nav.setSelectionForeground(Color.WHITE);
        nav.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) return;
            String name = nav.getSelectedValue();
            cards.show(content, name);
            ((Refreshable) modules.get(name)).refresh();
        });
        JScrollPane navScroll = new JScrollPane(nav);
        navScroll.setPreferredSize(new Dimension(190, 0));
        setLayout(new BorderLayout());
        add(navScroll, BorderLayout.WEST);
        add(content, BorderLayout.CENTER);
        nav.setSelectedIndex(0);
    }
}
