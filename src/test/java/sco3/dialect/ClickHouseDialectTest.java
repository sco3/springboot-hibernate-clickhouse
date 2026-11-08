package sco3.dialect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARCHAR;

import org.hibernate.type.SqlTypes;
import org.junit.jupiter.api.Test;

/**
 * Runs without a database; guards the Hibernate 7 dialect API contract and the
 * JDBC-to-ClickHouse type mapping.
 */
class ClickHouseDialectTest {

    private final ClickHouseDialect dialect = new ClickHouseDialect();

    @Test
    void mapsCommonJdbcTypesToClickHouseTypes() {
        assertThat(dialect.columnType(VARCHAR)).isEqualTo("String");
        assertThat(dialect.columnType(BIGINT)).isEqualTo("Int64");
        assertThat(dialect.columnType(DOUBLE)).isEqualTo("Float64");
        assertThat(dialect.columnType(UUID)).isEqualTo("UUID");
    }

    @Test
    void mapsTimestampsToDateTime64() {
        assertThat(dialect.columnType(TIMESTAMP)).isEqualTo("DateTime64(6, 'UTC')");
        assertThat(dialect.columnType(TIMESTAMP_WITH_TIMEZONE)).isEqualTo("DateTime64(6, 'UTC')");
    }

    @Test
    void mapsIntegerTypes() {
        assertThat(dialect.columnType(SqlTypes.INTEGER)).isEqualTo("Int32");
        assertThat(dialect.columnType(SqlTypes.TINYINT)).isEqualTo("Int8");
    }
}
