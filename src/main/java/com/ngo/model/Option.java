package com.ngo.model;

/** An (id, label) pair shown in a combo box for a foreign-key field. */
public final class Option {
    public final int id;
    public final String label;

    public Option(int id, String label) { this.id = id; this.label = label; }

    @Override public String toString() { return label; }
}
