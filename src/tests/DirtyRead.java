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

	public void updateBalance(int value1, int value2) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			
			
			PreparedStatement kir = connect.prepareStatement("select * from A where id=1");
			ResultSet kir_rs = kir.executeQuery();
			kir_rs.next();
			int	result = kir_rs.getInt("balance");
			
			
			
			
			PreparedStatement preparedStatement = connect.prepareStatement("update A set balance= ? where id=1");
			preparedStatement.setInt(1, value1);
			preparedStatement.executeUpdate();
			PreparedStatement preparedStatement2 = connect.prepareStatement("update A set balance= ? where id=?");
			preparedStatement2.setInt(1, value2);
			preparedStatement2.setInt(2, result);
			preparedStatement2.executeUpdate();
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
			PreparedStatement preparedStatement = connect.prepareStatement("select * from A where id=1");
			ResultSet rs = preparedStatement.executeQuery();
			int result = -1;
			if (rs.next()) {
				result = rs.getInt("balance");
			}
			System.out.println(result);
		} catch (Exception e) {
			throw e;
		} finally {

		}

	}
	
	
	
	
	

}
