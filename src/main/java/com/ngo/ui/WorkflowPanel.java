package com.ngo.ui;

import com.ngo.dao.LookupDao;
import com.ngo.model.Option;
import com.ngo.service.AssistanceService;
import com.ngo.service.EntityRegistry;
import com.ngo.util.AppException;
import com.ngo.util.ErrorHandler;
import com.ngo.util.Validator;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;
import javax.swing.*;

/**
 * One-click assistance workflow: create request + approve + distribute inside ONE JDBC transaction.
 * If the distribution is invalid the request and approval are rolled back as well.
 */
public class WorkflowPanel extends JPanel implements Refreshable {
    private final JComboBox<Option> beneficiary = new JComboBox<>(), programme = new JComboBox<>();
    private final JTextField description = new JTextField(30), requested = new JTextField(10),
            approved = new JTextField(10), distributed = new JTextField(10);
    private final JComboBox<String> type = new JComboBox<>(EntityRegistry.DIST_TYPES);
    private final JTextArea log = new JTextArea(8, 60);
    private String lastMessage = "";

    public WorkflowPanel() {
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JLabel t = new JLabel("Assistance Workflow (single transaction)");
        t.setFont(t.getFont().deriveFont(Font.BOLD, 18f)); t.setForeground(UiUtil.DARK);
        add(t, BorderLayout.NORTH);
        JPanel f = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 6, 5, 6); g.anchor = GridBagConstraints.WEST;
        String[] labels = {"Beneficiary *", "Programme *", "Request description *", "Requested amount *",
                "Approved amount *", "Distribute now *", "Distribution type *"};
        JComponent[] comps = {beneficiary, programme, description, requested, approved, distributed, type};
        for (int i = 0; i < labels.length; i++) {
            g.gridx = 0; g.gridy = i; f.add(new JLabel(labels[i]), g);
            g.gridx = 1; f.add(comps[i], g);
        }
        JButton run = new JButton("Create Request + Approve + Distribute");
        run.addActionListener(e -> run());
        g.gridx = 1; g.gridy = labels.length; f.add(run, g);
        JPanel north = new JPanel(new BorderLayout());
        north.add(f, BorderLayout.WEST);
        add(north, BorderLayout.CENTER);
        log.setEditable(false);
        log.setText("All three steps run in one transaction (setAutoCommit(false) -> commit / rollback).\n"
                  + "Try distributing more than the approved amount: the whole workflow is rolled back and no request is saved.\n");
        add(new JScrollPane(log), BorderLayout.SOUTH);
    }

    @Override public void refresh() {
        try {
            fill(beneficiary, LookupDao.options(EntityRegistry.LK_BENEFICIARY_PUBLIC));
            fill(programme, LookupDao.options("SELECT programme_id, programme_name FROM programme WHERE status = 'Active' ORDER BY programme_name"));
        } catch (Exception e) { report(ErrorHandler.message(e)); }
    }

    private void fill(JComboBox<Option> cb, List<Option> opts) {
        cb.removeAllItems();
        for (Option o : opts) cb.addItem(o);
    }

    public boolean run() {
        try {
            Option b = (Option) beneficiary.getSelectedItem(), p = (Option) programme.getSelectedItem();
            if (b == null || p == null) throw new AppException("Select a beneficiary and a programme.");
            String desc = Validator.required("Request description", description.getText());
            BigDecimal req = Validator.positive("Requested amount", Validator.decimal("Requested amount", Validator.required("Requested amount", requested.getText())));
            BigDecimal app = Validator.positive("Approved amount", Validator.decimal("Approved amount", Validator.required("Approved amount", approved.getText())));
            BigDecimal dis = Validator.positive("Distribution amount", Validator.decimal("Distribution amount", Validator.required("Distribution amount", distributed.getText())));
            int id = AssistanceService.fullWorkflow(b.id, p.id, desc, req, app, dis, (String) type.getSelectedItem());
            report("COMMITTED: request #" + id + " created, approved and distributed.");
            return true;
        } catch (Exception e) {
            report("ROLLED BACK: " + ErrorHandler.message(e) + " (nothing was saved)");
            return false;
        }
    }

    private void report(String m) { lastMessage = m; log.append(m + "\n"); }

    public String lastMessage() { return lastMessage; }
    public void set(int beneficiaryId, int programmeId, String desc, String req, String app, String dis) {
        select(beneficiary, beneficiaryId); select(programme, programmeId);
        description.setText(desc); requested.setText(req); approved.setText(app); distributed.setText(dis);
    }
    private void select(JComboBox<Option> cb, int id) {
        for (int i = 0; i < cb.getItemCount(); i++) if (cb.getItemAt(i).id == id) cb.setSelectedIndex(i);
    }
}
