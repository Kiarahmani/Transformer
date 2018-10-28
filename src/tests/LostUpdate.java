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

	public void updateBalance(int key) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");

			if (1==key)
			if (key == 123456) {
				PreparedStatement ps = connect.prepareStatement("select * from A where id=1");
				ResultSet rs1 = ps.executeQuery();
				if (key > 100)
					for (int i = 0; rs1.next(); i++) {
						int id = rs1.getInt("id");
						int balance = rs1.getInt("balance");
						ps = connect.prepareStatement("update A set balance = ? where id=?");
						ps.setInt(1, balance + 10000);
						ps.setInt(2, id + 1);
						ps.executeUpdate();
					}
			}
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
