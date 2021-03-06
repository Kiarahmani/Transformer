package tests;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class NonRepeatableRead {
	private Connection connect = null;
	private ResultSet rs = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int insID;
	Properties p;

	public NonRepeatableRead(int insID) {
		this.insID = insID;
		p = new Properties();
		p.setProperty("ID", String.valueOf(insID));
	}

	public void updateBalance(String zahremar, int key, int bal) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement ps = connect.prepareStatement("update A set balance= ? where id=?");
			ps.setInt(1, bal);
			ps.setInt(2, key);
			ps.executeUpdate();
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

	public void readBalance() throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement ps = connect.prepareStatement("select * from A where id=69");
			ResultSet rs2 = ps.executeQuery();
			rs2.next();
			int id = rs2.getInt("id");
			int balance = rs2.getInt("balance");
			System.out.println(id + balance);

			PreparedStatement ps1 = connect.prepareStatement("select * from A where id=69");
			ResultSet rs3 = ps1.executeQuery();
			rs3.next();
			int id1 = rs3.getInt("id");
			int balance1 = rs3.getInt("balance");
			System.out.println(id1 + balance1);

		} catch (Exception e) {
			throw e;
		} finally {

		}

	}
	
	public void readBalance2() throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement ps = connect.prepareStatement("select * from A where id=999");
			ResultSet rs2 = ps.executeQuery();
			rs2.next();
			int id = rs2.getInt("id");
			int balance = rs2.getInt("balance");
			System.out.println(id + balance);

			PreparedStatement ps1 = connect.prepareStatement("select * from A where id=999");
			ResultSet rs3 = ps1.executeQuery();
			rs3.next();
			int id1 = rs3.getInt("id");
			int balance1 = rs3.getInt("balance");
			System.out.println(id1 + balance1);

		} catch (Exception e) {
			throw e;
		} finally {

		}

	}


}
