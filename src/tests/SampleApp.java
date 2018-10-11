package tests;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

public class SampleApp {
	private Connection connect = null;
	private Statement stmt = null;
	private PreparedStatement preparedStatement = null;
	private ResultSet rs = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int insID;
	Properties p;

	public SampleApp(int insID) {
		this.insID = insID;
		p = new Properties();
		p.setProperty("ID", String.valueOf(insID));
	}

	public void increment(int key) throws Exception {
		try {

			Object o = Class.forName("MyDriver").newInstance();
			DriverManager.registerDriver((Driver) o);
			Driver driver = DriverManager.getDriver("jdbc:mydriver://");
			connect = driver.connect(null, p);
			connect.setAutoCommit(false);
			connect.setTransactionIsolation(_ISOLATION);
			preparedStatement = connect.prepareStatement("select * from feedback.kv where id=?");
			preparedStatement.setInt(1, key);
			rs = preparedStatement.executeQuery();
			rs.next();

			String query = "select * from feedback.kv where id=" + String.valueOf(key);
			connect.prepareStatement(query).executeQuery();
			System.out.println("(" + rs.getInt("id") + "," + rs.getInt("value") + ")");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close();
		}

	}

	private void close() {
		try {
			System.out.println("CLOSING DOWN!");
			if (rs != null)
				rs.close();
			if (rs != null)
				rs.close();
			if (connect != null)
				connect.close();
		} catch (Exception e) {

		}
	}

}
