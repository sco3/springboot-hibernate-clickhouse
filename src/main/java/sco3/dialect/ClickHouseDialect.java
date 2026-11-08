package sco3.dialect;

import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BINARY;
import static org.hibernate.type.SqlTypes.BIT;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.DECIMAL;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.FLOAT;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.LONGNVARCHAR;
import static org.hibernate.type.SqlTypes.LONGVARBINARY;
import static org.hibernate.type.SqlTypes.LONGVARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.REAL;
import static org.hibernate.type.SqlTypes.SMALLINT;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_UTC;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.UUID;
import static org.hibernate.type.SqlTypes.VARBINARY;
import static org.hibernate.type.SqlTypes.VARCHAR;

import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;

/**
 * Minimal Hibernate dialect for ClickHouse.
 */
public class ClickHouseDialect extends Dialect {

	public ClickHouseDialect() {
		super(DatabaseVersion.make(23, 8));
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		return switch (sqlTypeCode) {
		case BOOLEAN, BIT -> "Bool";
		case TINYINT -> "Int8";
		case SMALLINT -> "Int16";
		case INTEGER -> "Int32";
		case BIGINT -> "Int64";
		case FLOAT, REAL -> "Float32";
		case DOUBLE -> "Float64";
		case NUMERIC, DECIMAL -> "Decimal(38, 9)";
		case CHAR, NCHAR -> "String";
		case VARCHAR, NVARCHAR, LONGVARCHAR, LONGNVARCHAR, CLOB, NCLOB -> "String";
		case BINARY, VARBINARY, LONGVARBINARY, BLOB -> "String";
		case DATE -> "Date";
		case TIMESTAMP -> "DateTime64(6, 'UTC')";
		case TIMESTAMP_WITH_TIMEZONE, TIMESTAMP_UTC -> "DateTime64(6, 'UTC')";
		case UUID -> "UUID";
		default -> super.columnType(sqlTypeCode);
		};
	}
}
