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

public class SmallBank {
	private Connection connect = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int id;
	Properties p;

	public SmallBank(int id) {
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

	// ************************************************************************************
	public void amalgamate() throws SQLException {
	}
	// ************************************************************************************

	// ************************************************************************************
	public void balance(String custName) throws SQLException {
		// First convert the acctName to the acctId
		PreparedStatement stmt0 = connect.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE name = ?");
		stmt0.setString(1, custName);
		ResultSet r0 = stmt0.executeQuery();
		if (r0.next() == false) {
			String msg = "Invalid account '" + custName + "'";
			System.out.println(msg);
		}
		long custId = r0.getLong("custid");

		// Then get their account balances
		PreparedStatement balStmt0 = connect.prepareStatement("SELECT bal FROM " + "SAVINGS" + " WHERE custid = ?");
		balStmt0.setLong(1, custId);
		ResultSet balRes0 = balStmt0.executeQuery();
		if (balRes0.next() == false) {
			String msg = String.format("No %s for customer #%d", "SAVINGS", custId);
			System.out.println(msg);
		}

		PreparedStatement balStmt1 = connect.prepareStatement("SELECT bal FROM " + "CHECKING" + " WHERE custid = ?");
		balStmt1.setLong(1, custId);
		ResultSet balRes1 = balStmt1.executeQuery();
		if (balRes1.next() == false) {
			String msg = String.format("No %s for customer #%d", "CHECKING", custId);
			System.out.println(msg);
		}
	}
	// ***********************************************************************************

	// ************************************************************************************
	public void depositChecking(String custName, long amount) throws SQLException {
		// First convert the custName to the custId
		PreparedStatement stmt0 = connect.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE name = ?");
		stmt0.setString(1, custName);
		ResultSet r0 = stmt0.executeQuery();
		if (r0.next() == false) {
			String msg = "Invalid account '" + custName + "'";
			System.out.println(msg);
		}
		long custId = r0.getLong("custid");

		// Then update their checking balance
		PreparedStatement stmt1 = connect.prepareStatement("SELECT bal FROM " + "CHECKING" + " WHERE custid = ?");
		stmt1.setLong(1, custId);
		ResultSet r1 = stmt1.executeQuery();
		r1.next();
		long old_bal = r1.getLong("bal");

		PreparedStatement stmt2 = connect
				.prepareStatement("UPDATE " + "CHECKING" + " SET bal = ? " + " WHERE custid = ?");
		stmt2.setLong(1, old_bal + amount);
		stmt2.setLong(2, custId);

		int status = stmt2.executeUpdate();
		
	
		PreparedStatement stmt21 = connect
				.prepareStatement("UPDATE " + "CHECKING" + " SET bal = ? " + " WHERE custid = ?");
		stmt21.setLong(1, old_bal + amount+100);
		stmt21.setLong(2, custId);

		int status21 = stmt21.executeUpdate();
	}

	// ************************************************************************************
	public void sendPayment() throws SQLException {
	}

	// ************************************************************************************
	public void transactSavings() throws SQLException {
	}

	// ************************************************************************************
	public void writeCheck() throws SQLException {
	}

}
