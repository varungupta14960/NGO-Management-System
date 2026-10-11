package com.ngo.ui;

import com.ngo.dao.GenericDao;
import com.ngo.dao.LookupDao;
import com.ngo.model.EntityDef;
import com.ngo.model.FieldDef;
import com.ngo.model.Option;
import com.ngo.util.AppException;
import com.ngo.util.ErrorHandler;
import com.ngo.util.Validator;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.*;

/** Generic Add / View / Update / Delete / Search screen driven by an EntityDef. */
public class CrudPanel extends JPanel implements Refreshable {
    private final EntityDef entity;
    private final Map<String, JComponent> inputs = new LinkedHashMap<>();
    private final JTable table = UiUtil.table();
    private final JTextField searchField = new JTextField(22);
    private final JLabel status = new JLabel(" ");
    private Integer selectedId;

    public CrudPanel(EntityDef entity) {
        this.entity = entity;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(buildSearchBar(), BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.add(buildForm(), BorderLayout.CENTER);
        south.add(buildButtons(), BorderLayout.SOUTH);
        add(south, BorderLayout.SOUTH);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) onSelect(); });
        clearForm();
    }

    // ---------------------------------------------------------------- layout
    private JComponent buildSearchBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel title = new JLabel(entity.title + " Management   ");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        title.setForeground(UiUtil.DARK);
        JButton go = new JButton("Search"), reset = new JButton("Show All");
        go.addActionListener(e -> reload());
        searchField.addActionListener(e -> reload());
        reset.addActionListener(e -> { searchField.setText(""); reload(); });
        p.add(title); p.add(new JLabel("Search:")); p.add(searchField); p.add(go); p.add(reset);
        return p;
    }

    private JComponent buildForm() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createTitledBorder(entity.title + " details"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(3, 6, 3, 6); g.anchor = GridBagConstraints.WEST;
        int n = 0;
        for (FieldDef f : entity.fields) {
            JComponent c;
            if (f.kind == FieldDef.Kind.CHOICE) c = new JComboBox<>(f.choices);
            else if (f.kind == FieldDef.Kind.LOOKUP) { JComboBox<Option> cb = new JComboBox<>(); cb.setPrototypeDisplayValue(new Option(0, "x".repeat(34))); c = cb; }
            else c = new JTextField(f.maxLength > 100 ? 28 : 16);
            inputs.put(f.column, c);
            int col = (n % 2) * 2, row = n / 2;
            g.gridx = col; g.gridy = row; g.fill = GridBagConstraints.NONE;
            form.add(new JLabel(f.label + (f.required ? " *" : "")), g);
            g.gridx = col + 1; g.fill = GridBagConstraints.HORIZONTAL; g.weightx = 1;
            form.add(c, g);
            n++;
        }
        return form;
    }

    private JComponent buildButtons() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        boolean custom = entity.insertHandler != null;
        JButton add = new JButton(custom && entity.table.equals("donation") ? "Record Donation" : "Add");
        add.addActionListener(e -> doAdd());
        p.add(add);
        if (entity.allowUpdate) { JButton b = new JButton("Update"); b.addActionListener(e -> doUpdate()); p.add(b); }
        if (entity.allowDelete) { JButton b = new JButton("Delete"); b.addActionListener(e -> doDelete()); p.add(b); }
        JButton clear = new JButton("Clear"); clear.addActionListener(e -> clearForm()); p.add(clear);
        status.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 0));
        p.add(status);
        return p;
    }

    // ---------------------------------------------------------------- data
    @Override public void refresh() { reloadLookups(); reload(); }

    private void reload() {
        try {
            UiUtil.apply(table, UiUtil.model(GenericDao.list(entity, searchField.getText())));
            selectedId = null;
        } catch (Exception e) { fail(e); }
    }

    @SuppressWarnings("unchecked")
    private void reloadLookups() {
        for (FieldDef f : entity.fields) {
            if (f.kind != FieldDef.Kind.LOOKUP) continue;
            JComboBox<Option> cb = (JComboBox<Option>) inputs.get(f.column);
            Option previous = (Option) cb.getSelectedItem();
            try {
                List<Option> opts = LookupDao.options(f.lookupSql);
                cb.removeAllItems();
                if (!f.required) cb.addItem(new Option(0, "-- none --"));
                for (Option o : opts) cb.addItem(o);
                if (previous != null) selectLookup(f.column, previous.id);
                else if (cb.getItemCount() > 0) cb.setSelectedIndex(f.required ? -1 : 0);
            } catch (Exception e) { fail(e); }
        }
    }

    private void onSelect() {
        int row = table.getSelectedRow();
        if (row < 0) return;
        selectedId = ((Number) table.getValueAt(row, 0)).intValue();
        if (!entity.allowUpdate) return;
        try {
            Map<String, Object> v = GenericDao.find(entity, selectedId);
            if (v == null) return;
            for (FieldDef f : entity.fields) setValue(f, v.get(f.column));
            ok("Selected " + entity.title + " #" + selectedId);
        } catch (Exception e) { fail(e); }
    }

    @SuppressWarnings("unchecked")
    private void setValue(FieldDef f, Object val) {
        JComponent c = inputs.get(f.column);
        if (f.kind == FieldDef.Kind.LOOKUP) selectLookup(f.column, val == null ? 0 : ((Number) val).intValue());
        else if (f.kind == FieldDef.Kind.CHOICE) ((JComboBox<String>) c).setSelectedItem(val == null ? f.defaultValue : val.toString());
        else ((JTextField) c).setText(val == null ? "" : (val instanceof BigDecimal ? ((BigDecimal) val).toPlainString() : val.toString()));
    }

    // ---------------------------------------------------------------- actions (public so tests can drive them)
    public void doAdd() {
        try {
            Map<String, Object> v = collect();
            if (entity.insertHandler != null) entity.insertHandler.insert(v);
            else GenericDao.insert(entity, v);
            ok(entity.title + " saved successfully.");
            clearForm(); reloadLookups(); reload();
        } catch (Exception e) { fail(e); }
    }

    public void doUpdate() {
        if (selectedId == null) { fail(new AppException("Select a row in the table first.")); return; }
        try {
            GenericDao.update(entity, selectedId, collect());
            ok(entity.title + " #" + selectedId + " updated.");
            reloadLookups(); reload();
        } catch (Exception e) { fail(e); }
    }

    public void doDelete() {
        if (selectedId == null) { fail(new AppException("Select a row in the table first.")); return; }
        if (!UiUtil.confirm(this, "Delete " + entity.title + " #" + selectedId + "?")) return;
        try {
            GenericDao.delete(entity, selectedId);
            ok(entity.title + " #" + selectedId + " deleted.");
            clearForm(); reloadLookups(); reload();
        } catch (Exception e) { fail(e); }
    }

    private Map<String, Object> collect() throws AppException {
        Map<String, Object> v = new LinkedHashMap<>();
        for (FieldDef f : entity.fields) {
            JComponent c = inputs.get(f.column);
            Object raw = c instanceof JTextField ? ((JTextField) c).getText() : ((JComboBox<?>) c).getSelectedItem();
            v.put(f.column, Validator.field(f, raw));
        }
        return v;
    }

    @SuppressWarnings("unchecked")
    public void clearForm() {
        selectedId = null;
        table.clearSelection();
        for (FieldDef f : entity.fields) {
            JComponent c = inputs.get(f.column);
            if (f.kind == FieldDef.Kind.LOOKUP) { JComboBox<Option> cb = (JComboBox<Option>) c; if (cb.getItemCount() > 0) cb.setSelectedIndex(f.required ? -1 : 0); }
            else if (f.kind == FieldDef.Kind.CHOICE) ((JComboBox<String>) c).setSelectedItem(f.defaultValue);
            else if (f.kind == FieldDef.Kind.DATE && f.required && !f.column.equals("end_date")) ((JTextField) c).setText(LocalDate.now().toString());
            else ((JTextField) c).setText(f.defaultValue);
        }
    }

    // ---------------------------------------------------------------- helpers for status + tests
    private void ok(String m) { status.setForeground(UiUtil.GREEN); status.setText(m); }

    private void fail(Exception e) {
        String m = ErrorHandler.message(e);
        status.setForeground(Color.RED.darker()); status.setText(m);
        UiUtil.error(this, m);
    }

    public String getStatus() { return status.getText(); }
    public int getRowCount() { return table.getRowCount(); }
    public void setText(String column, String text) { ((JTextField) inputs.get(column)).setText(text); }
    @SuppressWarnings("unchecked")
    public void setChoice(String column, String value) { ((JComboBox<String>) inputs.get(column)).setSelectedItem(value); }
    @SuppressWarnings("unchecked")
    public void selectLookup(String column, int id) {
        JComboBox<Option> cb = (JComboBox<Option>) inputs.get(column);
        for (int i = 0; i < cb.getItemCount(); i++) if (cb.getItemAt(i).id == id) { cb.setSelectedIndex(i); return; }
    }
    public void search(String text) { searchField.setText(text); reload(); }
    public void selectRowById(int id) {
        for (int r = 0; r < table.getRowCount(); r++)
            if (((Number) table.getValueAt(r, 0)).intValue() == id) { table.setRowSelectionInterval(r, r); return; }
    }
}
