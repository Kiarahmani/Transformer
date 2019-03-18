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


	public void updateBalance() throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");

			PreparedStatement ps ;
			
			ps = connect.prepareStatement("update A set balance = ? where id=1");
			ps.setInt(1, 10);
			ps.executeUpdate();
			
			ps = connect.prepareStatement("update A set balance = ? where id=1");
			ps.setInt(1, 11);
			ps.executeUpdate();
			
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}
	
	
	public void kir() throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");

			PreparedStatement ps = connect.prepareStatement("select balance from A where id=1");
			ResultSet rs = ps.executeQuery();
			rs.next();
			int balance = rs.getInt("balance");
			System.out.println(balance);
			
			
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
