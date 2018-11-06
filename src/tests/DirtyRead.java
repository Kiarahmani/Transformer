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

public class DirtyRead {
	private Connection connect = null;
	private ResultSet rs = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int insID;
	Properties p;

	public DirtyRead(int insID) {
		this.insID = insID;
		p = new Properties();
		p.setProperty("ID", String.valueOf(insID));
	}

	public void updateBalance(int bal1, int bal2) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement ps = connect.prepareStatement("update A set balance= ? where id=1");
			ps.setInt(1, bal1);
			ps.executeUpdate();
			PreparedStatement ps2 = connect.prepareStatement("update A set balance= ? where id=1");
			ps2.setInt(1, bal2);
			ps2.executeUpdate();
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

	public void readBalance(int key) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement ps = connect.prepareStatement("select * from A where id=1");
			ResultSet rs2 = ps.executeQuery();
			rs2.next();
			int id = rs2.getInt("id");
			int balance = rs2.getInt("balance");
			System.out.println(id + balance);
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
