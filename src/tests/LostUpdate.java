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

public class LostUpdate {
	private Connection connect = null;
	private ResultSet rs = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int insID;
	Properties p;

	public LostUpdate(int insID) {
		this.insID = insID;
		p = new Properties();
		p.setProperty("ID", String.valueOf(insID));
	}

	public void updateBalance(int key, String name, boolean shouldPrint, double anotherDouble) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement ps = connect.prepareStatement("select * from A where id=1");
			rs = ps.executeQuery("select * from A where id=1");
			rs.next();
			int id = rs.getInt("id");
			int balance = rs.getInt("balance");
			ps = connect.prepareStatement("update A set balance= 5 where id=1");
			ps.executeUpdate("update A set balance= 5 where id=1");
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
