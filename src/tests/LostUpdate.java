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


	public void updateBalance(int val) throws Exception {
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
			
			ps = connect.prepareStatement("select * from A where id=?");
			ps.setInt(1, 1);
			rs = ps.executeQuery();
			rs.next();
			int balance2 = rs.getInt("balance");
			System.out.println(balance2);

			
			
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
