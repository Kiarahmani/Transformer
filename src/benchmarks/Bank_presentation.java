package benchmarks;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Bank_presentation {
	private Connection connect = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int id;
	Properties p;

	public Bank_presentation(int id) {
		this.id = id;
		p = new Properties();
		p.setProperty("id", String.valueOf(this.id));
		Object o;
		try {
			o = Class.forName("MyDriver").newInstance();
			DriverManager.registerDriver((Driver) o);
			Driver driver = DriverManager.getDriver("jdbc:mydriver://");
			connect = driver.connect("", p);
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
	}

	//
	// *****************
	public void deposit(int id, int amount) throws SQLException {
		PreparedStatement stmt = connect.prepareStatement("SELECT balance " + "  FROM " + "ACCOUNTS" + " WHERE ID = ?");
		stmt.setInt(1, id);
		ResultSet rs = stmt.executeQuery();
		if (!rs.next()) {
			System.out.println("ERROR: Invalid account id: " + id);
		}
		// rs.next();
		int old_bal = rs.getInt("balance");
		PreparedStatement stmt2 = connect.prepareStatement("UPDATE ACCOUNTS SET BALANCE = ?" + " WHERE ID = ?");
		stmt2.setInt(1, old_bal + amount);
		stmt2.setInt(2, id);
		stmt2.executeUpdate();

	}
/*
	//
	// ******************
	public void withdraw(int id, int amount) throws SQLException {
		PreparedStatement stmt = connect.prepareStatement("SELECT balance " + "  FROM " + "ACCOUNTS" + " WHERE ID = ?");
		stmt.setInt(1, id);
		ResultSet rs = stmt.executeQuery();
		if (!rs.next()) {
			System.out.println("ERROR: Invalid account id: " + id);
		}
		int old_bal = rs.getInt("balance");
		if (old_bal >= amount) {
			PreparedStatement stmt2 = connect.prepareStatement("UPDATE ACCOUNTS SET BALANCE = ?" + " WHERE ID = ?");
			stmt2.setInt(1, old_bal - amount);
			stmt2.setInt(2, id);
			stmt2.executeUpdate();
		}

	}
*/
}
