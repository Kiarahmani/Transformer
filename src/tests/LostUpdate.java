package tests;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

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

	public void updateBalance(int key, String name, boolean shouldPrint, double anotherDouble) throws Exception {
		try {

			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement ps = connect.prepareStatement("select C_ID_STR,C_BALANCE from CUSTOMER where C_ID = ?");
			int x = 230;
			ps.setInt(1, key + x);
			ResultSet rs = ps.executeQuery();
			rs.next();
			int id1 = rs.getInt("C_ID_STR");
			System.out.println(125 * id1);
			rs.next();
			int sum = id1 + rs.getInt(3);
			System.out.println("SUM: " + sum);
			rs.close();

			ResultSet rs2;
			PreparedStatement ps2;
			ps2 = connect.prepareStatement("select C_BALANCE from CUSTOMER where C_ID = ?");
			ps2.setInt(1, sum);
			rs2 = ps2.executeQuery();
			rs2.next();
			int balance = rs2.getInt("C_BALANCE");
			System.out.println(rs2);
			rs2.close();

			PreparedStatement ps3 = connect.prepareStatement(
					"update DEPARTMENT set D_Name = Broke,D_FUNDS = ? where D_ID = 1 and D_FUNDS > = 10000");

			ps3.setInt(1, balance);
			ps3.executeUpdate();
			ps3.close();

			PreparedStatement ps4 = connect.prepareStatement("select * from DEPARTMENT where D_ID >= ?");
			ps4.setInt(1, sum);
			ResultSet rs4 = ps4.executeQuery();
			rs4.next();
			long someLong = rs4.getLong("D_FUNDS");
			System.out.println(rs4);

			PreparedStatement ps5 = connect.prepareStatement(
					"INSERT INTO DEPARTMENT (D_ID, D_ADDR, D_FUNDS, D_NAME) VALUES (5,Yeager2550, ?,Sales)");
			ps5.setLong(1, someLong);
			ps5.executeUpdate();
			ps5.close();

		} catch (Exception e) {
			throw e;
		} finally {

		}

	}

}
