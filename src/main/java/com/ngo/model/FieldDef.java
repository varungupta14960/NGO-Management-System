package com.ngo.model;

/** Describes one column of an entity: label, data kind and validation rules. */
public final class FieldDef {
    public enum Kind { TEXT, INT, DECIMAL, DATE, CHOICE, LOOKUP }
    public enum Check { NONE, EMAIL, PHONE }

    public final String column, label;
    public final Kind kind;
    public boolean required = true;
    public boolean positive = false;     // DECIMAL must be > 0 (otherwise >= 0)
    public int maxLength = 255;
    public Check check = Check.NONE;
    public String[] choices;             // CHOICE values
    public String lookupSql;             // LOOKUP: SELECT id, label ...
    public String defaultValue = "";

    private FieldDef(String column, String label, Kind kind) { this.column = column; this.label = label; this.kind = kind; }

    public static FieldDef text(String col, String label, int max) { FieldDef f = new FieldDef(col, label, Kind.TEXT); f.maxLength = max; return f; }
    public static FieldDef email(String col, String label) { FieldDef f = text(col, label, 100); f.check = Check.EMAIL; return f; }
    public static FieldDef phone(String col, String label) { FieldDef f = text(col, label, 15); f.check = Check.PHONE; return f; }
    public static FieldDef money(String col, String label) { FieldDef f = new FieldDef(col, label, Kind.DECIMAL); f.positive = true; return f; }
    public static FieldDef amount0(String col, String label) { return new FieldDef(col, label, Kind.DECIMAL); }
    public static FieldDef date(String col, String label) { return new FieldDef(col, label, Kind.DATE); }
    public static FieldDef choice(String col, String label, String... values) { FieldDef f = new FieldDef(col, label, Kind.CHOICE); f.choices = values; f.defaultValue = values[0]; return f; }
    public static FieldDef lookup(String col, String label, String sql) { FieldDef f = new FieldDef(col, label, Kind.LOOKUP); f.lookupSql = sql; return f; }

    public FieldDef optional() { this.required = false; return this; }
    public FieldDef dflt(String v) { this.defaultValue = v; return this; }
}
