package benchmarks;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class TPCC {
	private Connection conn = null;
	private int _ISOLATION = Connection.TRANSACTION_READ_COMMITTED;
	private int insID;
	Properties p;

	public TPCC(int insID) {
		this.insID = insID;
		p = new Properties();
		p.setProperty("ID", String.valueOf(insID));
	}

	/*
	 * 
	 * GET FOLLOWERS
	 * 
	 */
	public void newOrder(int w_id, int d_id, int c_id, int o_all_local, int o_ol_cnt, int t_current, int[] itemIDs,
			int[] supplierWarehouseIDs, int[] orderQuantities) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			conn = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");

			// datastructures required for bookkeeping
			int[] itemPrices = new int[o_ol_cnt];
			String[] itemNames = new String[o_ol_cnt];
			int[] stockQuantities = new int[o_ol_cnt];
			int[] orderLineAmounts = new int[o_ol_cnt];
			int total_amount = 0;
			char[] brandGeneric = new char[o_ol_cnt];

			// retrieve w_tax rate
			PreparedStatement stmt_w = conn
					.prepareStatement("SELECT W_TAX " + "  FROM " + "WAREHOUSE" + " WHERE W_ID = ?");
			stmt_w.setInt(1, w_id);
			ResultSet w_rs = stmt_w.executeQuery();
			if (!w_rs.next()) {
				System.out.println("ERROR_11: Invalid warehouse id: " + w_id);
			}
			int w_tax = w_rs.getInt("W_TAX");
			w_rs.close();
			//
			// retrieve d_tax rate and update D_NEXT_O_ID
			PreparedStatement stmt_d = conn.prepareStatement(
					"SELECT D_NEXT_O_ID, D_TAX " + "  FROM " + "DISTRICT" + " WHERE D_W_ID = ? AND D_ID = ?");
			stmt_d.setInt(1, w_id);
			stmt_d.setInt(2, d_id);
			ResultSet d_rs = stmt_d.executeQuery();
			if (!d_rs.next()) {
				System.out.println("ERROR_12: Invalid district id: (" + w_id + "," + d_id + ")");
			}
			int d_next_o_id = d_rs.getInt("D_NEXT_O_ID");
			int d_tax = d_rs.getInt("D_TAX");
			int o_id = d_next_o_id;

			PreparedStatement stmt_u_d = conn.prepareStatement(
					"UPDATE " + "DISTRICT" + "   SET D_NEXT_O_ID = ? " + " WHERE D_W_ID = ? " + "   AND D_ID = ?");
			stmt_u_d.setInt(1, d_next_o_id + 1);
			stmt_u_d.setInt(2, w_id);
			stmt_u_d.setInt(3, d_id);
			stmt_u_d.executeUpdate();

			PreparedStatement stmt_i_o = conn.prepareStatement(
					"INSERT INTO " + "OORDER" + " (O_ID, O_D_ID, O_W_ID, O_C_ID, O_ENTRY_D, O_OL_CNT, O_ALL_LOCAL)"
							+ " VALUES (?, ?, ?, ?, ?, ?, ?)");
			stmt_i_o.setInt(1, o_id);
			stmt_i_o.setInt(2, d_id);
			stmt_i_o.setInt(3, w_id);
			stmt_i_o.setInt(5, t_current);
			stmt_i_o.setInt(6, o_ol_cnt);
			stmt_i_o.setInt(7, o_all_local);
			stmt_i_o.executeUpdate();
			//
			PreparedStatement stmt_i_no = conn.prepareStatement(
					"INSERT INTO " + "NEW_ORDER" + " (NO_O_ID, NO_D_ID, NO_W_ID) " + " VALUES ( ?, ?, ?)");
			stmt_i_no.setInt(1, o_id);
			stmt_i_no.setInt(2, d_id);
			stmt_i_no.setInt(3, w_id);
			stmt_i_no.executeUpdate();

			//
			// retrieve customer's information
			PreparedStatement stmt_c = conn.prepareStatement("SELECT C_DISCOUNT, C_LAST, C_CREDIT" + "  FROM "
					+ "CUSTOMER" + " WHERE C_W_ID = ? " + "   AND C_D_ID = ? " + "   AND C_ID = ?");
			stmt_c.setInt(1, w_id);
			stmt_c.setInt(2, d_id);
			stmt_c.setInt(3, c_id);
			ResultSet c_rs = stmt_c.executeQuery();
			if (!c_rs.next()) {
				System.out.println("ERROR_13: Invalid customer id: (" + w_id + "," + d_id + "," + c_id + ")");
			}
			int c_discount = c_rs.getInt("C_DISCOUNT");
			String c_last = c_rs.getString("C_LAST");
			String c_credit = c_rs.getString("C_CREDIT");

			for (int ol_number = 1; ol_number <= o_ol_cnt; ol_number++) {
				int ol_supply_w_id = supplierWarehouseIDs[ol_number - 1];
				int ol_i_id = itemIDs[ol_number - 1];
				int ol_quantity = orderQuantities[ol_number - 1];
				PreparedStatement stmt_i = conn.prepareStatement(
						"SELECT I_ID, I_PRICE, I_NAME , I_DATA " + "  FROM " + "ITEM" + " WHERE I_ID = ?");
				stmt_i.setInt(1, ol_i_id);
				ResultSet i_rs = stmt_i.executeQuery();
				// this is expected to happen 1% of the times
				if (!i_rs.next()) {
					if (ol_number != o_ol_cnt) {
						System.out.println("ERROR_14: Invalid item id: (" + ol_i_id
								+ ") given in the middle of the order list (unexpected)");
					}
					System.out.println("EXPECTED_ERROR_15: Invalid item id: (" + ol_i_id + ")");
				}
				int i_price = i_rs.getInt("I_PRICE");
				String i_name = i_rs.getString("I_NAME");
				String i_data = i_rs.getString("I_DATA");
				itemPrices[ol_number - 1] = i_price;
				itemNames[ol_number - 1] = i_name;
				// retrieve stock
				PreparedStatement stmt_s = conn
						.prepareStatement("SELECT  *  FROM " + "STOCK" + " WHERE S_I_ID = ? " + "   AND S_W_ID = ?");
				stmt_s.setInt(1, i_rs.getInt("I_ID"));
				stmt_s.setInt(2, ol_supply_w_id);
				ResultSet s_rs = stmt_s.executeQuery();
				if (!s_rs.next()) {
					System.out.println("ERROR_16: Invalid stock primary key: (" + ol_i_id + "," + ol_supply_w_id + ")");
				}
				int s_quantity = s_rs.getInt("S_QUANTITY");
				int s_ytd = s_rs.getInt("S_YTD");
				int s_order_cnt = s_rs.getInt("S_ORDER_CNT");
				int s_remote_cnt = s_rs.getInt("S_REMOTE_CNT");
				String s_data = s_rs.getString("S_DATA");
				String s_dist_01 = s_rs.getString("S_DIST_01");
				String s_dist_02 = s_rs.getString("S_DIST_02");
				String s_dist_03 = s_rs.getString("S_DIST_03");
				String s_dist_04 = s_rs.getString("S_DIST_04");
				String s_dist_05 = s_rs.getString("S_DIST_05");
				String s_dist_06 = s_rs.getString("S_DIST_06");
				String s_dist_07 = s_rs.getString("S_DIST_07");
				String s_dist_08 = s_rs.getString("S_DIST_08");
				String s_dist_09 = s_rs.getString("S_DIST_09");
				String s_dist_10 = s_rs.getString("S_DIST_10");
				s_rs.close();
				//
				stockQuantities[ol_number - 1] = s_quantity;
				if (s_quantity - ol_quantity >= 10) {
					s_quantity -= ol_quantity; // new s_quantity
				} else {
					s_quantity += -ol_quantity + 91; // new s_quantity
				}
				int s_remote_cnt_increment;
				if (ol_supply_w_id == w_id) {
					s_remote_cnt_increment = 0;
				} else {
					s_remote_cnt_increment = 1;
				}

				// update stock row
				PreparedStatement stmtUpdateStock = conn.prepareStatement(
						"UPDATE " + "STOCK" + " SET S_QUANTITY = ?," + "S_YTD = ?," + "S_ORDER_CNT = ?,"
								+ "S_REMOTE_CNT = ? " + " WHERE S_I_ID = ? " + "   AND S_W_ID = ?");
				stmtUpdateStock.setInt(1, s_quantity);
				stmtUpdateStock.setInt(2, s_ytd + ol_quantity);
				stmtUpdateStock.setInt(3, s_order_cnt + 1);
				stmtUpdateStock.setInt(4, s_remote_cnt + s_remote_cnt_increment);
				stmtUpdateStock.setInt(5, ol_i_id);
				stmtUpdateStock.setInt(6, ol_supply_w_id);
				stmtUpdateStock.executeUpdate();
				//
				int ol_amount = ol_quantity * i_price;
				orderLineAmounts[ol_number - 1] = ol_amount;
				total_amount += ol_amount;
				if (i_data.indexOf("ORIGINAL") != -1 && s_data.indexOf("ORIGINAL") != -1) {
					brandGeneric[ol_number - 1] = 'B';
				} else {
					brandGeneric[ol_number - 1] = 'G';
				}
				String ol_dist_info = "";
				if (d_id == 1)
					ol_dist_info = s_dist_01;
				if (d_id == 2)
					ol_dist_info = s_dist_02;
				if (d_id == 3)
					ol_dist_info = s_dist_03;
				if (d_id == 4)
					ol_dist_info = s_dist_04;
				if (d_id == 5)
					ol_dist_info = s_dist_05;
				if (d_id == 6)
					ol_dist_info = s_dist_06;
				if (d_id == 7)
					ol_dist_info = s_dist_07;
				if (d_id == 8)
					ol_dist_info = s_dist_08;
				if (d_id == 9)
					ol_dist_info = s_dist_09;
				if (d_id == 10)
					ol_dist_info = s_dist_10;
				//
				// insert a row into orderline table representing each order item
				PreparedStatement i_stmt = conn.prepareStatement("INSERT INTO " + "ORDER_LINE"
						+ " (OL_O_ID, OL_D_ID, OL_W_ID, OL_NUMBER, OL_I_ID, OL_SUPPLY_W_ID, OL_QUANTITY, OL_AMOUNT, OL_DIST_INFO) "
						+ " VALUES (?,?,?,?,?,?,?,?,?)");

				i_stmt.setInt(1, o_id);
				i_stmt.setInt(2, d_id);
				i_stmt.setInt(3, w_id);
				i_stmt.setInt(4, ol_number);
				i_stmt.setInt(5, ol_i_id);
				i_stmt.setInt(6, ol_supply_w_id);
				i_stmt.setInt(7, ol_quantity);
				i_stmt.setDouble(8, ol_amount);
				i_stmt.setString(9, ol_dist_info);
				i_stmt.executeUpdate();
			}
			total_amount *= (1 + w_tax + d_tax) * (1 - c_discount);
			// stmt.clearBatch();
			//
			// ❄❄❄❄❄❄❄❄❄❄❄❄❄❄❄
			// TXN SUCCESSFUL!
			// ❄❄❄❄❄❄❄❄❄❄❄❄❄❄❄

		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	/*
	 * public void payment() throws Exception { try {
	 * Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
	 * System.out.println("connecting..."); conn =
	 * DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID +
	 * "/testks");
	 * 
	 * } catch (Exception e) { throw e; } finally { } }
	 * 
	 * 
	 * public void stockLevel() throws Exception { try {
	 * Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
	 * System.out.println("connecting..."); conn =
	 * DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID +
	 * "/testks");
	 * 
	 * } catch (Exception e) { throw e; } finally { } }
	 * 
	 * 
	 * public void orderStatus() throws Exception { try {
	 * Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
	 * System.out.println("connecting..."); conn =
	 * DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID +
	 * "/testks");
	 * 
	 * } catch (Exception e) { throw e; } finally { } }
	 * 
	 * 
	 * public void delivery() throws Exception { try {
	 * Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
	 * System.out.println("connecting..."); conn =
	 * DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID +
	 * "/testks");
	 * 
	 * } catch (Exception e) { throw e; } finally { } }
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
