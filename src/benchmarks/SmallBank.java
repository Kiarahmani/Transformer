package benchmarks;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

	// 1
	// ************************************************************************************
	public void amalgamate(int custId0, int custId1) throws SQLException {
		// Get Account Information
		PreparedStatement stmt0 = connect.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE custid = ?");
		stmt0.setInt(1, custId0);
		ResultSet r0 = stmt0.executeQuery();
		if (r0.next() == false) {
			String msg = "Invalid account '" + custId0 + "'";
			System.out.println(msg);
			return;
		}

		PreparedStatement stmt1 = connect.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE custid = ?");
		stmt1.setInt(1, custId1);
		ResultSet r1 = stmt1.executeQuery();
		if (r1.next() == false) {
			String msg = "Invalid account '" + custId0 + "'";
			System.out.println(msg);
			return;
		}

		// Get Balance Information
		PreparedStatement balStmt0 = connect.prepareStatement("SELECT bal FROM " + "SAVINGS" + " WHERE custid = ?");
		balStmt0.setInt(1, custId0);
		ResultSet balRes0 = balStmt0.executeQuery();
		if (balRes0.next() == false) {
			String msg = String.format("No %s for customer #%d", "SAVINGS", custId0);
			System.out.println(msg);
			return;
		}
		int old_sv_cust0 = balRes0.getInt("bal");

		// Get Balance Information
		PreparedStatement balStmt1 = connect.prepareStatement("SELECT bal FROM " + "CHECKING" + " WHERE custid = ?");
		balStmt1.setInt(1, custId1);
		ResultSet balRes1 = balStmt1.executeQuery();
		if (balRes1.next() == false) {
			String msg = String.format("No %s for customer #%d", "CHECKING", custId0);
			System.out.println(msg);
			return;
		}
		int old_ck_cust1 = balRes1.getInt("bal");

		// sum of the checking and saving accounts read above
		int total = old_sv_cust0 + old_ck_cust1;

		// Update Balance Information
		PreparedStatement updateStmt0 = connect
				.prepareStatement("UPDATE " + "CHECKING" + " SET bal = 0 " + "WHERE custid = ?");
		updateStmt0.setInt(1, custId0);
		updateStmt0.executeUpdate();

		PreparedStatement updateStmt10 = connect.prepareStatement("SELECT bal FROM " + "SAVINGS" + " WHERE custid = ?");
		updateStmt10.setInt(1, custId1);
		ResultSet balRes10 = updateStmt10.executeQuery();
		if (balRes10.next() == false) {
			String msg = String.format("No %s for customer #%d", "SAVINGS", custId1);
			System.out.println(msg);
			return;
		}
		int old_bal = balRes10.getInt("bal");

		PreparedStatement updateStmt11 = connect
				.prepareStatement("UPDATE " + "SAVINGS" + "   SET bal =  ? " + " WHERE custid = ?");
		updateStmt11.setInt(1, old_bal - total);
		updateStmt11.setInt(2, custId1);
		updateStmt11.executeUpdate();

	}

	// ***********************************************************************************
	//
	//
	//
	//
	//
	//
	//
	//
	// ************************************************************************************
	public void balance(String custName) throws SQLException {
		PreparedStatement stmt0 = connect.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE name = ?");
		stmt0.setString(1, custName);
		ResultSet r0 = stmt0.executeQuery();
		if (r0.next() == false) {
			String msg = "Invalid account '" + custName + "'";
			System.out.println(msg);
			return;
		}
		int custId = r0.getInt("custid");

		// Then get their account balances
		PreparedStatement balStmt0 = connect.prepareStatement("SELECT bal FROM " + "SAVINGS" + " WHERE custid = ?");
		balStmt0.setInt(1, custId);
		ResultSet balRes0 = balStmt0.executeQuery();
		if (balRes0.next() == false) {
			String msg = String.format("No %s for customer #%d", "SAVINGS", custId);
			System.out.println(msg);
			return;
		}
		int savings = balRes0.getInt("bal");

		PreparedStatement balStmt1 = connect.prepareStatement("SELECT bal FROM " + "CHECKING" + " WHERE custid = ?");
		balStmt1.setInt(1, custId);
		ResultSet balRes1 = balStmt1.executeQuery();
		if (balRes1.next() == false) {
			String msg = String.format("No %s for customer #%d", "CHECKING", custId);
			System.out.println(msg);
			return;
		}
		int checking = balRes1.getInt("bal");

		// show the results
		System.out.println(savings + checking);

	}

	// ***********************************************************************************
	//
	//
	//
	//
	//
	//
	//
	//
	// ************************************************************************************
	public void depositChecking(String custName, int amount) throws SQLException {
		// First convert the custName to the custId
		PreparedStatement stmt0 = connect.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE name = ?");
		stmt0.setString(1, custName);
		ResultSet r0 = stmt0.executeQuery();
		if (r0.next() == false) {
			String msg = "Invalid account '" + custName + "'";
			System.out.println(msg);
			return;
		}
		int custId = r0.getInt("custid");

		// Then update their checking balance
		PreparedStatement stmt10 = connect.prepareStatement("SELECT * FROM " + "CHECKING" + " WHERE custid = ?");
		stmt10.setInt(1, custId);
		ResultSet r10 = stmt10.executeQuery();
		if (r10.next() == false) {
			String msg = "Invalid checking '" + custId + "'";
			System.out.println(msg);
			return;
		}
		int old_bal = r10.getInt("bal");
		PreparedStatement stmt11 = connect.prepareStatement("UPDATE CHECKING SET bal = ? " + " WHERE custid = ?");
		stmt11.setInt(1, old_bal - amount);
		stmt11.setInt(2, custId);
		stmt11.executeUpdate();

	}

	// ***********************************************************************************
	//
	//
	//
	//
	//
	//
	//
	//
	// ************************************************************************************
	public void sendPayment(int sendAcct, int destAcct, int amount) throws SQLException {
		PreparedStatement stmt0 = connect.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE custid = ?");
		stmt0.setInt(1, sendAcct);
		ResultSet r0 = stmt0.executeQuery();
		if (r0.next() == false) {
			String msg = "Invalid account '" + sendAcct + "'";
			System.out.println(msg);
			return;
		}

		PreparedStatement stmt1 = connect.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE custid = ?");
		stmt1.setInt(1, destAcct);
		ResultSet r1 = stmt1.executeQuery();
		if (r1.next() == false) {
			String msg = "Invalid account '" + sendAcct + "'";
			System.out.println(msg);
			return;
		}

		// Get the sender's account balance
		PreparedStatement balStmt0 = connect.prepareStatement("SELECT bal FROM " + "CHECKING" + " WHERE custid = ?");
		balStmt0.setInt(1, sendAcct);
		ResultSet balRes0 = balStmt0.executeQuery();
		if (balRes0.next() == false) {
			String msg = String.format("No %s for customer #%d", "CHECKING", sendAcct);
			System.out.println(msg);
			return;
		}
		int old_bal_send = balRes0.getInt("bal");

		// Make sure that they have enough money
		if (old_bal_send > amount) {
			// Update the sender's account balance
			PreparedStatement deptStmt = connect.prepareStatement("UPDATE CHECKING SET bal = ? WHERE custid = ?");
			deptStmt.setInt(1, old_bal_send - amount);
			deptStmt.setInt(2, sendAcct);
			deptStmt.executeUpdate();

			// Get the receivers's account balance
			PreparedStatement crdtStmt1 = connect
					.prepareStatement("SELECT bal FROM " + "CHECKING" + " WHERE custid = ?");
			crdtStmt1.setInt(1, destAcct);
			ResultSet balRes1 = crdtStmt1.executeQuery();
			if (balRes1.next() == false) {
				String msg = String.format("No %s for customer #%d", "CHECKING", destAcct);
				System.out.println(msg);
				return;
			}
			int old_bal_dest = balRes1.getInt("bal");

			// Update the receivers's account balance
			PreparedStatement crdtStmt2 = connect.prepareStatement("UPDATE CHECKING SET bal = ? WHERE custid = ?");
			crdtStmt2.setInt(1, old_bal_dest + amount);
			crdtStmt2.setInt(2, destAcct);
			crdtStmt2.executeUpdate();

		}

	}

	// ***********************************************************************************
	//
	//
	//
	//
	//
	//
	//
	//
	// ************************************************************************************
	public void transactSavings(String custName, int amount) throws SQLException {
		// First convert the custName to the custId
		PreparedStatement stmt0 = connect.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE name = ?");
		stmt0.setString(1, custName);
		ResultSet r0 = stmt0.executeQuery();
		if (r0.next() == false) {
			String msg = "Invalid account '" + custName + "'";
			System.out.println(msg);
			return;
		}
		int custId = r0.getInt("custid");

		// Get the account balance
		PreparedStatement balStmt0 = connect.prepareStatement("SELECT bal FROM " + "SAVINGS" + " WHERE custid = ?");
		balStmt0.setInt(1, custId);
		ResultSet balRes0 = balStmt0.executeQuery();
		if (balRes0.next() == false) {
			String msg = String.format("No %s for customer #%d", "CHECKING", custId);
			System.out.println(msg);
			return;
		}
		// Update the balance
		int bal = balRes0.getInt("bal") - amount;
		if (bal > 0) {
			PreparedStatement deptStmt = connect.prepareStatement("UPDATE SAVINGS SET bal = ? WHERE custid = ?");
			deptStmt.setInt(1, bal);
			deptStmt.setInt(2, custId);
			deptStmt.executeUpdate();
		}
	}

	// ***********************************************************************************
	//
	//
	//
	//
	//
	//
	//
	//
	// ************************************************************************************
	public void writeCheck(String custName, int amount) throws SQLException {
		// First convert the custName to the custId
		PreparedStatement stmt0 = connect.prepareStatement("SELECT * FROM " + "ACCOUNTS" + " WHERE name = ?");
		stmt0.setString(1, custName);
		ResultSet r0 = stmt0.executeQuery();
		if (r0.next() == false) {
			String msg = "Invalid account '" + custName + "'";
			System.out.println(msg);
			return;
		}
		int custId = r0.getInt("custid");

		// Then get their account balances
		PreparedStatement balStmt0 = connect.prepareStatement("SELECT bal FROM " + "SAVINGS" + " WHERE custid = ?");
		balStmt0.setInt(1, custId);
		ResultSet balRes0 = balStmt0.executeQuery();
		if (balRes0.next() == false) {
			String msg = String.format("No %s for customer #%d", "SAVINGS", custId);
			System.out.println(msg);
			return;
		}
		int savings = balRes0.getInt("bal");

		PreparedStatement balStmt1 = connect.prepareStatement("SELECT bal FROM " + "CHECKING" + " WHERE custid = ?");
		balStmt1.setInt(1, custId);
		ResultSet balRes1 = balStmt1.executeQuery();
		if (balRes1.next() == false) {
			String msg = String.format("No %s for customer #%d", "CHECKING", custId);
			System.out.println(msg);
			return;
		}
		int checking = balRes1.getInt("bal");

		if (savings + checking > amount) {
			// Update the receivers's account balance
			PreparedStatement crdtStmt1 = connect.prepareStatement("UPDATE CHECKING SET bal = ? WHERE custid = ?");
			crdtStmt1.setInt(1, checking - amount);
			crdtStmt1.setInt(2, custId);
			crdtStmt1.executeUpdate();
		} else {
			// Update the receivers's account balance
			PreparedStatement crdtStmt2 = connect.prepareStatement("UPDATE CHECKING SET bal = ? WHERE custid = ?");
			crdtStmt2.setInt(1, checking - (amount - 1));
			crdtStmt2.setInt(2, custId);
			crdtStmt2.executeUpdate();
		}

	}

}
