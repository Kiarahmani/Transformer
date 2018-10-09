package tests;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
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
		int x = key * 2;
		// System.out.println(x);
		x = key * 100;
		// System.out.println(x);

	}

	public void doubleUpdate(int key) throws Exception {
		try {
			Object o = Class.forName("MyDriver").newInstance();
			DriverManager.registerDriver((Driver) o);
			Driver driver = DriverManager.getDriver("jdbc:mydriver://");
			connect = driver.connect(null, p);
			connect.setAutoCommit(false);
			connect.setTransactionIsolation(_ISOLATION);
			// update 1
			preparedStatement = connect.prepareStatement("update feedback.kv set value=1000 where id=?");
			preparedStatement.setInt(1, key);
			preparedStatement.executeUpdate();

			// update 2
			preparedStatement = connect.prepareStatement("update feedback.kv set value=2000 where id=?");
			preparedStatement.setInt(1, key);
			// Thread.sleep(5000);
			preparedStatement.executeUpdate();
			connect.commit();
		} catch (Exception e) {
			throw e;
		} finally {
			close();
		}

	}

	public void select(int key) {
		try {

			Object o = Class.forName("MyDriver").newInstance();
			DriverManager.registerDriver((Driver) o);
			Driver driver = DriverManager.getDriver("jdbc:mydriver://");
			connect = driver.connect(null, p);
			connect.setAutoCommit(false);
			connect.setTransactionIsolation(_ISOLATION);
			preparedStatement = connect.prepareStatement("select * from feedback.kv where id=?");
			preparedStatement.setInt(1, key);
			// Thread.sleep(2500);
			rs = preparedStatement.executeQuery();
			rs.next();
			System.out.println("(" + rs.getInt("id") + "," + rs.getInt("value") + ")");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			close();
		}
	}

	public void initialize(int key) throws Exception {
		try {
			Object o = Class.forName("MyDriver").newInstance();
			DriverManager.registerDriver((Driver) o);
			Driver driver = DriverManager.getDriver("jdbc:mydriver://");
			connect = driver.connect(null, p);
			preparedStatement = connect.prepareStatement("update feedback.kv set value=0 where id=?");
			preparedStatement.setInt(1, key);
			preparedStatement.executeUpdate();
		} catch (Exception e) {
			throw e;
		} finally {
			close();
		}

	}

	private void close() {
		try {
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
