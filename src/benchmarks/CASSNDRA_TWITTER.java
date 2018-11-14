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
import java.util.concurrent.ThreadLocalRandom;

public class CASSNDRA_TWITTER {
	private Connection connect = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int insID;
	Properties p;

	public CASSNDRA_TWITTER(int insID) {
		this.insID = insID;
		p = new Properties();
		p.setProperty("ID", String.valueOf(insID));
	}

	/*
	 * 
	 * 
	 */
	public void registerUser(String username, String password,String username2) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement stmt = connect.prepareStatement("INSERT INTO USERS VALUES (?,?)");
			stmt.setString(1, username);
			stmt.setString(2, password);
			stmt.executeUpdate();
		} catch (

		Exception e) {
			throw e;
		} finally {
		}
	}

	
	 /*
	public void followUser(String followerUsername, String followedUsername) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			Long unixTime = System.currentTimeMillis();
			PreparedStatement stmt1 = connect.prepareStatement("INSERT INTO FOLLOWERS VALUES (?,?,?)");
			stmt1.setString(1, followedUsername);
			stmt1.setString(2, followerUsername);
			stmt1.setLong(3, unixTime);
			stmt1.executeUpdate();

			PreparedStatement stmt2 = connect.prepareStatement("INSERT INTO FRIENDS VALUES (?,?,?)");
			stmt2.setString(1, followerUsername);
			stmt2.setString(2, followedUsername);
			stmt2.setLong(3, unixTime);
			stmt2.executeUpdate();
		} catch (

		Exception e) {
			throw e;
		} finally {
		}
	}
*/
	/*
	 * 
	 * 
	 */
	public void insertTweet(String username, String tweet,String followerUsername, String followedUsername) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			Long timeId = System.currentTimeMillis();
			int tweetId = ThreadLocalRandom.current().nextInt(0, 10001);

			PreparedStatement stmt1 = connect.prepareStatement("INSERT INTO TWEETS VALUES (?,?,?)");
			stmt1.setInt(1, tweetId);
			stmt1.setString(2, username);
			stmt1.setString(3, tweet);
			stmt1.executeUpdate();

			PreparedStatement stmt2 = connect.prepareStatement("INSERT INTO USERLINE VALUES (?,?,?)");
			stmt2.setString(1, username);
			stmt2.setLong(2, timeId);
			stmt2.setInt(3, tweetId);
			stmt2.executeUpdate();

			PreparedStatement stmt3 = connect.prepareStatement("INSERT INTO TIMELINE VALUES (?,?,?)");
			stmt3.setString(1, username);
			stmt3.setLong(2, timeId);
			stmt3.setInt(3, tweetId);
			stmt3.executeUpdate();

			PreparedStatement stmt4 = connect.prepareStatement("SELECT follower FROM FOLLOWERS WHERE username = ?");
			stmt4.setString(1, username);
			ResultSet results4 = stmt4.executeQuery();
			while (results4.next()) {
				PreparedStatement stmt5 = connect.prepareStatement("INSERT INTO TIMELINE VALUES (?,?,?)");
				stmt5.setString(1, results4.getString("follower"));
				stmt5.setLong(2, timeId);
				stmt5.setInt(3, tweetId);
				stmt5.executeUpdate();
			}
			
			
			

		} catch (

		Exception e) {
			throw e;
		} finally {
		}
	}

	/*
	 * 
	 * 
	 */
	/*
	public void getUserline(String username) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			Long timeId = System.currentTimeMillis();
			int tweetId = ThreadLocalRandom.current().nextInt(0, 10001);

			PreparedStatement stmt1 = connect.prepareStatement("SELECT tweet_id FROM USERLINE WHERE username = ?");
			stmt1.setString(1, username);
			ResultSet results1 = stmt1.executeQuery();
			while (results1.next()) {
				PreparedStatement stmt2 = connect
						.prepareStatement("SELECT username, body FROM TWEETS WHERE tweet_id = ?");
				stmt2.setInt(1, results1.getInt("tweet_id"));
				ResultSet results2 = stmt2.executeQuery();
				results2.next();
			}

		} catch (

		Exception e) {
			throw e;
		} finally {
		}
	}
	
*/
	/*
	public void getTimeline(String username) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			connect = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");

			PreparedStatement stmt1 = connect.prepareStatement("SELECT tweet_id FROM TIMELINE WHERE username = ?");
			stmt1.setString(1, username);
			ResultSet results1 = stmt1.executeQuery();
			
			while (results1.next()) {
				PreparedStatement stmt2 = connect
						.prepareStatement("SELECT username, body FROM TWEETS WHERE tweet_id = ?");
				stmt2.setInt(1, results1.getInt("tweet_id"));
				ResultSet results2 = stmt2.executeQuery();
				results2.next();
			}

		} catch (

		Exception e) {
			throw e;
		} finally {
		}
	}
*/
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
