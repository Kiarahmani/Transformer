package tests;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

public class LostUpdate {
	private Connection connect = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int insID;
	Properties p;

	public LostUpdate(int insID) {
		this.insID = insID;
		p = new Properties();
		p.setProperty("ID", String.valueOf(insID));
	}

	public void readingShit(int val) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");

			PreparedStatement ps = connect.prepareStatement("select * from A where id=?");
			ps.setInt(1, 1);
			ResultSet rs = ps.executeQuery();
			rs.next();
			int balance = rs.getInt("balance");
			System.out.println(balance);
			
			PreparedStatement ps2 = connect.prepareStatement("select * from A where id=?");
			ps2.setInt(1, 1);
			ResultSet rs2 = ps2.executeQuery();
			rs2.next();
			int balance2 = rs2.getInt("balance");
			System.out.println(balance2);
			
			

		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

	public void updateBalance2(int val) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");

			PreparedStatement ps = connect.prepareStatement("update A set balance = ? where id=?");
			ps.setInt(1, val);
			ps.setInt(2, 1);
			ps.executeUpdate();

		} catch (Exception e) {
			throw e;
		} finally {

		}

	}
	
	public void updateBalance3(int val) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");

			PreparedStatement ps = connect.prepareStatement("update A set balance = ? where id=?");
			ps.setInt(1, val);
			ps.setInt(2, 1);
			ps.executeUpdate();

			ps = connect.prepareStatement("update A set balance = ? where id=?");
			ps.setInt(1, val * 2);
			ps.setInt(2, 1);
			ps.executeUpdate();

		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
