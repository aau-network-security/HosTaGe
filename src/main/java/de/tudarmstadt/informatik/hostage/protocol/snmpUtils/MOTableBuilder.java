package de.tudarmstadt.informatik.hostage.protocol.snmpUtils;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.mo.DefaultMOMutableRow2PC;
import org.snmp4j.agent.mo.DefaultMOTable;
import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.agent.mo.MOMutableTableModel;
import org.snmp4j.agent.mo.MOTable;
import org.snmp4j.agent.mo.MOTableIndex;
import org.snmp4j.agent.mo.MOTableSubIndex;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.SMIConstants;
import org.snmp4j.smi.Variable;

import java.util.ArrayList;
import java.util.List;

/**
 * Module to build the managed object table for SNMP
 */
public class MOTableBuilder {

    private MOTableSubIndex[] subIndexes = new MOTableSubIndex[] { new MOTableSubIndex(
            SMIConstants.SYNTAX_INTEGER) };
    private MOTableIndex indexDef = new MOTableIndex(subIndexes, false);

    private final List<MOColumn> columns = new ArrayList<MOColumn>();
    private final List<Variable[]> tableRows = new ArrayList<Variable[]>();
    private int currentRow = 0;
    private int currentCol = 0;

    private OID tableRootOid;

    private int colTypeCnt = 0;


    /**
     * Specified oid is the root oid of this table
     */
    public MOTableBuilder(OID oid) {
        this.tableRootOid = oid;
    }

    /**
     * Adds all column types {@link MOColumn} to this table.
     * Important to understand that you must add all types here before
     * adding any row values
     *
     * @param syntax use {@link SMIConstants}
     * @param access
     * @return
     */
    public MOTableBuilder addColumnType(int syntax, MOAccess access) {
        colTypeCnt++;
        columns.add(new MOColumn(colTypeCnt, syntax, access));
        return this;
    }


    public MOTableBuilder addRowValue(Variable variable) {
        if (tableRows.size() == currentRow) {
            tableRows.add(new Variable[columns.size()]);
        }
        tableRows.get(currentRow)[currentCol] = variable;
        currentCol++;
        if (currentCol >= columns.size()) {
            currentRow++;
            currentCol = 0;
        }
        return this;
    }

    public MOTable build() {
        DefaultMOTable ifTable = new DefaultMOTable(tableRootOid, indexDef,
                columns.toArray(new MOColumn[0]));
        MOMutableTableModel model = (MOMutableTableModel) ifTable.getModel();
        int i = 1;

        for (Variable[] variables : tableRows) {
            model.addRow(new DefaultMOMutableRow2PC(new OID(String.valueOf(i)),
                    variables));
            i++;
        }
        ifTable.setVolatile(true);
        return ifTable;
    }
}
