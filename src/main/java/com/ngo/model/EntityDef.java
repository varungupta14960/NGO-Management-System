package com.ngo.model;

import java.util.Map;

/** Describes a table for the generic CRUD screen: columns, display query, search and permitted actions. */
public final class EntityDef {
    /** Custom insert logic (e.g. a JDBC transaction in a service). Values are already validated/converted. */
    public interface InsertHandler { void insert(Map<String, Object> values) throws Exception; }

    public final String title, table, pk;
    public final FieldDef[] fields;
    public String listSql;       // SELECT ... FROM ... (first column MUST be the primary key)
    public String searchExpr;    // expression searched with LIKE ?
    public String orderBy;       // ORDER BY expression
    public boolean allowUpdate = true, allowDelete = true;
    public InsertHandler insertHandler;   // null = plain INSERT through GenericDao

    public EntityDef(String title, String table, String pk, FieldDef... fields) {
        this.title = title; this.table = table; this.pk = pk; this.fields = fields;
    }

    public EntityDef list(String sql, String search, String order) { listSql = sql; searchExpr = search; orderBy = order; return this; }
    public EntityDef readOnlyRows() { allowUpdate = false; allowDelete = false; return this; }
    public EntityDef noUpdate() { allowUpdate = false; return this; }
    public EntityDef onInsert(InsertHandler h) { insertHandler = h; return this; }
}
