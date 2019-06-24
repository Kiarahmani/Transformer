package benchmarks;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Twitter {
	private Connection connect = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int insID;
	Properties p;

	public Twitter(int insID) {
		this.insID = insID;
		p = new Properties();
		p.setProperty("ID", String.valueOf(insID));
	}

	/*
	 * 
	 * GET FOLLOWERS
	 * 
	 */
	public void getFollowers(long uid) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement stmt = connect.prepareStatement("SELECT f2 FROM FOLLOWERS WHERE f1 = ? ");
			stmt.setLong(1, uid);
			ResultSet rs = stmt.executeQuery();
			long last = -1;
			while (rs.next()) {
				PreparedStatement stmt2 = connect.prepareStatement("SELECT uid, name FROM USER_PROFILES WHERE uid = ?");
				last = rs.getLong(1);
				stmt2.setLong(1, last);
				ResultSet rs2 = stmt2.executeQuery();
				rs2.next();
				System.out.println(rs2.getInt("uid") + rs2.getString("name"));
			} // WHILE
			rs.close();
		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	/*
	 * 
	 * GET TWEETS
	 * 
	 */
	public void getTweets(long tweet_id) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement stmt = connect.prepareStatement("SELECT * FROM TWEETS WHERE id = ?");
			stmt.setLong(1, tweet_id);
			ResultSet rs = stmt.executeQuery();
			rs.next();
			System.out.println(rs.getString("text"));
			rs.close();
		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	/*
	 * 
	 * Get Tweets From Following
	 * 
	 */
	public void getTweetsFromFollowing(int uid) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement stmt1 = connect.prepareStatement("SELECT f2 FROM FOLLOWS WHERE f1 = ?");
			stmt1.setInt(1, uid);
			ResultSet rs1 = stmt1.executeQuery();
			while (rs1.next()) {
				PreparedStatement stmt2 = connect.prepareStatement("SELECT * FROM TWEETS WHERE uid = ?");
				int last = rs1.getInt("f2");
				stmt2.setInt(1, last);
				ResultSet rs2 = stmt2.executeQuery();
				rs2.next();
				System.out.println(rs2.getInt("uid") + rs2.getString("text"));
			} // WHILE

		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	/*
	 * 
	 * Get User Tweets
	 * 
	 */
	public void getUserTweets(long uid) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement stmt1 = connect.prepareStatement("SELECT * FROM TWEETS WHERE uid = ?");
			stmt1.setLong(1, uid);
			ResultSet rs1 = stmt1.executeQuery();
			while (rs1.next())
				System.out.println(rs1.getString("text"));
		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	/*
	 * 
	 * Insert Tweet
	 * 
	 */
	public void insertTweet(long uid, String text, String time) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");

			PreparedStatement stmt0 = connect.prepareStatement("SELECT id FROM TWEETS WHERE uid=?");
			stmt0.setLong(1, uid);
			ResultSet rs0 = stmt0.executeQuery();
			rs0.next();

			PreparedStatement stmt1 = connect
					.prepareStatement("INSERT INTO TWEETS (id, uid,text,createdate) VALUES (?, ?, ?, ?)");
			stmt1.setLong(1, rs0.getLong("id")+1);
			stmt1.setLong(2, uid);
			stmt1.setString(3, text);
			stmt1.setString(4, time);
			stmt1.executeUpdate();
		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

}

/*
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 * 
 */
