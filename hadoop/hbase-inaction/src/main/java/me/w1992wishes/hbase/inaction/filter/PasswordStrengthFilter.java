package me.w1992wishes.hbase.inaction.filter;

import me.w1992wishes.hbase.common.dao.UsersDAO;
import org.apache.hadoop.hbase.Cell;
import org.apache.hadoop.hbase.CellUtil;
import org.apache.hadoop.hbase.filter.FilterBase;

/**
 * @author w1992wishes 2019/7/20 14:25
 */
public class PasswordStrengthFilter extends FilterBase {

    private int len;
    private boolean filterRow = false;

    // This flag is used to speed up seeking cells when matched column is found, such that following
    // columns in the same row can be skipped faster by NEXT_ROW instead of NEXT_COL.
    private boolean columnFound = false;

    public PasswordStrengthFilter(int len) {
        this.len = len;
    }

    @Override
    public ReturnCode filterCell(final Cell c) {
        if (!CellUtil.matchingColumn(c, UsersDAO.INFO_FAM, UsersDAO.PASS_COL)) {
            return columnFound ? ReturnCode.NEXT_ROW : ReturnCode.NEXT_COL;
        }
        // Column found
        columnFound = true;
        if(c.getValueLength() >= len) {
            this.filterRow = true;
            return ReturnCode.SKIP;
        }
        return ReturnCode.INCLUDE;
    }

    @Override
    public boolean filterRow() {
        return this.filterRow;
    }

    @Override
    public void reset() {
        this.filterRow = false;
        columnFound = false;
    }

}
