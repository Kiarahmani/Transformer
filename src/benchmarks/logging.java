package benchmarks;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class logging {
	private Connection connect = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int id;
	Properties p;


	//
	// *****************
	public void doStuff(int id, int amount) throws SQLException {
		PreparedStatement stmt = connect.prepareStatement("SELECT val " + "  FROM " + "Logs" + " WHERE ID = ?");
	
		stmt.setInt(1, id);
		ResultSet rs = stmt.executeQuery();
		if (!rs.next()) {
			System.out.println("ERROR: Invalid account id: " + id);
		}
		// rs.next();
		int old_val = rs.getInt("val");
		PreparedStatement stmt2 = connect.prepareStatement("UPDATE Logs SET val = ?" + " WHERE ID = ?");
		stmt2.setInt(1, old_val + amount);
		stmt2.setInt(2, id);
		stmt2.executeUpdate();

	}

}
