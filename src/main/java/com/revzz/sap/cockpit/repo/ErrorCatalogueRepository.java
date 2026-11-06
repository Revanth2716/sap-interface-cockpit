package com.revzz.sap.cockpit.repo;

import com.revzz.sap.cockpit.domain.ErrorCode;
import com.revzz.sap.cockpit.domain.Severity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/** The error catalogue: what each error code means and what to do about it. */
@Repository
public class ErrorCatalogueRepository {

    private final JdbcTemplate jdbc;

    public ErrorCatalogueRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<ErrorCode> MAPPER = new RowMapper<ErrorCode>() {
        @Override
        public ErrorCode mapRow(ResultSet rs, int rowNum) throws SQLException {
            ErrorCode code = new ErrorCode();
            code.setCode(rs.getString("ERR_CODE"));
            code.setSeverity(Severity.valueOf(rs.getString("SEVERITY")));
            code.setCause(rs.getString("CAUSE"));
            code.setRemediation(rs.getString("REMEDIATION"));
            code.setCommon("Y".equalsIgnoreCase(rs.getString("COMMON")));
            return code;
        }
    };

    public List<ErrorCode> findAll() {
        return jdbc.query("SELECT ERR_CODE, SEVERITY, CAUSE, REMEDIATION, COMMON FROM IFERR "
                + "ORDER BY SEVERITY, ERR_CODE", MAPPER);
    }

    public ErrorCode findByCode(String code) {
        List<ErrorCode> rows = jdbc.query("SELECT ERR_CODE, SEVERITY, CAUSE, REMEDIATION, COMMON "
                + "FROM IFERR WHERE ERR_CODE = ?", MAPPER, code);
        return rows.isEmpty() ? null : rows.get(0);
    }
}
