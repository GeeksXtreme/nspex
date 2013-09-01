package org.whired.nspex.blackbox.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.whired.nspex.tools.logging.Log;

/**
 * Simplifies SQLite database operations
 * @author Whired
 */
public class SQLiteDatabase {

	private static final String DRIVER = "org.sqlite.JDBC";
	private static final String PROTOCOL = "jdbc:sqlite:";

	private final Connection connection;
	private final String databaseName;

	public SQLiteDatabase(final String workingDir, final String databaseName) throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class.forName(DRIVER);
		final Properties connectionProperties = new Properties();
		connectionProperties.put("user", "program");
		connectionProperties.put("password", "program");
		this.databaseName = databaseName;
		this.connection = DriverManager.getConnection(PROTOCOL + workingDir + this.databaseName, connectionProperties);
		connection.setAutoCommit(false);
		Log.l.info("Database '" + databaseName + "' initialized");
	}

	/**
	 * Commits any statements to the database
	 * @throws SQLException
	 */
	public void commit() throws SQLException {
		this.connection.commit();
	}

	/**
	 * Executes the specified statement
	 * @param statement the statement to execute
	 * @throws SQLException when a statement fails to execute
	 */
	public void executeStatement(final String statement, boolean commitNow) throws SQLException {
		final Statement s = connection.createStatement();
		s.execute(statement);
		s.close();
		if (commitNow) {
			commit();
		}

	}

	public void executePreparedStatement(final String statement, final boolean commitNow) throws SQLException {
		final PreparedStatement ps = connection.prepareStatement(statement);
		ps.executeUpdate();
		ps.close();
		if (commitNow) {
			commit();
		}
	}

	/**
	 * Executes the specified query
	 * @param query the query to execute
	 * @return the results returned by the query
	 * @throws SQLException if the database can not be queried
	 */
	public ResultSet executeQuery(final String query) throws SQLException {
		final Statement s = connection.createStatement();
		return s.executeQuery(query);
	}

	@Override
	protected void finalize() throws Throwable {
		close();
		super.finalize();
	}

	/**
	 * Closes the database
	 */
	public void close() {
		try {
			connection.close();
		}
		catch (final SQLException ex) {
		}
	}
}