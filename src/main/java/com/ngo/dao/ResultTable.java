package com.ngo.dao;

import java.util.List;

/** Plain holder for query results shown in a JTable. */
public final class ResultTable {
    public final String[] columns;
    public final List<Object[]> rows;

    public ResultTable(String[] columns, List<Object[]> rows) { this.columns = columns; this.rows = rows; }
}
