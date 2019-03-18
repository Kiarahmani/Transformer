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

			PreparedStatement stmt_i_o = conn.prepareStatement("INSERT INTO " + "OORDER"
					+ " (O_ID, O_D_ID, O_W_ID, O_C_ID, O_CARRIER_ID, O_ENTRY_D, O_OL_CNT, O_ALL_LOCAL)"
					+ " VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
			stmt_i_o.setInt(1, o_id);
			stmt_i_o.setInt(2, d_id);
			stmt_i_o.setInt(3, w_id);
			stmt_i_o.setInt(4, c_id);
			stmt_i_o.setInt(5, -1);
			stmt_i_o.setInt(6, t_current);
			stmt_i_o.setInt(7, o_ol_cnt);
			stmt_i_o.setInt(8, o_all_local);
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
				int i_id = i_rs.getInt("I_ID");
				String i_name = i_rs.getString("I_NAME");
				String i_data = i_rs.getString("I_DATA");
				itemPrices[ol_number - 1] = i_price;
				itemNames[ol_number - 1] = i_name;
				// retrieve stock
				PreparedStatement stmt_s = conn
						.prepareStatement("SELECT  *  FROM " + "STOCK" + " WHERE S_I_ID = ? " + "   AND S_W_ID = ?");
				stmt_s.setInt(1, i_id);
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
				stmtUpdateStock.setInt(5, i_id);
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
						+ " VALUES (?,?,?,?,?,?,?,?,?,?)");

				i_stmt.setInt(1, o_id);
				i_stmt.setInt(2, d_id);
				i_stmt.setInt(3, w_id);
				i_stmt.setInt(4, ol_number);
				i_stmt.setInt(5, i_id);
				i_stmt.setInt(6, -1);
				i_stmt.setInt(7, ol_supply_w_id);
				i_stmt.setInt(8, ol_quantity);
				i_stmt.setInt(9, ol_amount);
				i_stmt.setString(10, ol_dist_info);
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

	public void payment(int w_id, int d_id, int customerByName, int c_id, String c_last, int customerWarehouseID,
			int customerDistrictID, int paymentAmount, int current_time) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			conn = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			boolean isRemote = (w_id != customerDistrictID);
			int w_ydt, d_ytd;
			String w_street_1, w_street_2, w_city, w_state, w_zip, w_name;
			String d_street_1, d_street_2, d_city, d_state, d_zip, d_name;
			// read necessary columns from warehouse
			PreparedStatement stmt_w = conn
					.prepareStatement("SELECT W_YTD, W_STREET_1, W_STREET_2, W_CITY, W_STATE, W_ZIP, W_NAME" + "  FROM "
							+ "WAREHOUSE" + " WHERE W_ID = ?");
			stmt_w.setInt(1, w_id);
			ResultSet w_rs = stmt_w.executeQuery();
			if (!w_rs.next()) {
				System.out.println("ERROR_21: Invalid warehouse id: " + w_id);
			}
			w_ydt = w_rs.getInt("W_YTD");
			w_street_1 = w_rs.getString("W_STREET_1");
			w_street_2 = w_rs.getString("W_STREET_2");
			w_city = w_rs.getString("W_CITY");
			w_state = w_rs.getString("W_STATE");
			w_zip = w_rs.getString("W_ZIP");
			w_name = w_rs.getString("W_NAME");
			w_rs.close();
			//
			// update W_YTD by paymentAmount
			PreparedStatement stmt_w_u = conn
					.prepareStatement("UPDATE " + "WAREHOUSE" + "   SET W_YTD = ? " + " WHERE W_ID = ? ");
			stmt_w_u.setInt(1, w_ydt + paymentAmount);
			stmt_w_u.setInt(2, w_id);
			stmt_w_u.executeUpdate();

			//
			// read necessary columns from district
			PreparedStatement stmt_d = conn
					.prepareStatement("SELECT D_YTD, D_STREET_1, D_STREET_2, D_CITY, D_STATE, D_ZIP, D_NAME" + "  FROM "
							+ "DISTRICT" + " WHERE D_W_ID = ? " + "   AND D_ID = ?");
			stmt_d.setInt(1, w_id);
			stmt_d.setInt(2, d_id);
			ResultSet d_rs = stmt_d.executeQuery();
			if (!d_rs.next()) {
				System.out.println("ERROR_22: Invalid district id: " + w_id + "," + d_id);
			}
			d_ytd = d_rs.getInt("D_YTD");
			d_street_1 = d_rs.getString("D_STREET_1");
			d_street_2 = d_rs.getString("D_STREET_2");
			d_city = d_rs.getString("D_CITY");
			d_state = d_rs.getString("D_STATE");
			d_zip = d_rs.getString("D_ZIP");
			d_name = d_rs.getString("D_NAME");
			d_rs.close();

			//
			// update D_YTD by paymentAmount
			PreparedStatement stmt_d_u = conn
					.prepareStatement("UPDATE " + "DISTRICT" + "   SET D_YTD = ? " + " WHERE D_W_ID = ? AND D_ID = ? ");
			stmt_d_u.setInt(1, d_ytd + paymentAmount);
			stmt_d_u.setInt(2, w_id);
			stmt_d_u.setInt(3, d_id);
			stmt_d_u.executeUpdate();

			String c_first, c_middle, c_street_1, c_street_2, c_city, c_state, c_zip, c_phone, c_credit, c_data;
			int c_credit_lim, c_discount, c_balance;
			int c_ytd_payment;
			int c_payment_cnt;
			Timestamp c_since;
			if (customerByName == 0) {
				PreparedStatement stmt_c = conn.prepareStatement("SELECT " + "*" + "  FROM " + "CUSTOMER"
						+ " WHERE C_W_ID = ? " + "   AND C_D_ID = ? " + "   AND C_LAST = ? ");
				stmt_c.setInt(1, customerWarehouseID);
				stmt_c.setInt(2, customerDistrictID);
				stmt_c.setString(3, c_last);
				ResultSet c_rs_2 = stmt_c.executeQuery();
				c_rs_2.next();
				c_first = c_rs_2.getString("c_first");
				c_middle = c_rs_2.getString("c_middle");
				c_street_1 = c_rs_2.getString("c_street_1");
				c_street_2 = c_rs_2.getString("c_street_2");
				c_city = c_rs_2.getString("c_city");
				c_state = c_rs_2.getString("c_state");
				c_zip = c_rs_2.getString("c_zip");
				c_phone = c_rs_2.getString("c_phone");
				c_credit = c_rs_2.getString("c_credit");
				c_credit_lim = c_rs_2.getInt("c_credit_lim");
				c_discount = c_rs_2.getInt("c_discount");
				c_balance = c_rs_2.getInt("c_balance");
				c_ytd_payment = c_rs_2.getInt("c_ytd_payment");
				c_payment_cnt = c_rs_2.getInt("c_payment_cnt");
				c_since = c_rs_2.getTimestamp("c_since");
				c_data = c_rs_2.getString("C_DATA");
				int new_c_id = c_rs_2.getInt("C_ID");
				c_rs_2.close();

				// Update customers info
				PreparedStatement stmt_c_u = conn.prepareStatement("UPDATE " + "CUSTOMER" + "   SET C_BALANCE = ?, "
						+ "       C_YTD_PAYMENT = ?, " + "       C_PAYMENT_CNT = ?, " + "       C_DATA = ? "
						+ " WHERE C_W_ID = ? " + "   AND C_D_ID = ? " + "   AND C_ID = ?");
				stmt_c_u.setInt(1, c_balance + (-1 * paymentAmount));
				stmt_c_u.setInt(2, c_ytd_payment + paymentAmount);
				stmt_c_u.setInt(3, c_payment_cnt + 1);
				stmt_c_u.setString(4, "updated data");
				stmt_c_u.setInt(5, customerWarehouseID);
				stmt_c_u.setInt(6, customerDistrictID);
				stmt_c_u.setInt(7, new_c_id);
				stmt_c_u.executeUpdate();

			} else {
				PreparedStatement stmt_c = conn.prepareStatement("SELECT " + "*" + "  FROM " + "CUSTOMER"
						+ " WHERE C_W_ID = ? " + "   AND C_D_ID = ? " + "   AND C_ID = ? ");
				stmt_c.setInt(1, customerWarehouseID);
				stmt_c.setInt(2, customerDistrictID);
				stmt_c.setInt(3, c_id);
				ResultSet c_rs_2 = stmt_c.executeQuery();
				c_rs_2.next();
				c_first = c_rs_2.getString("c_first");
				c_middle = c_rs_2.getString("c_middle");
				c_street_1 = c_rs_2.getString("c_street_1");
				c_street_2 = c_rs_2.getString("c_street_2");
				c_city = c_rs_2.getString("c_city");
				c_state = c_rs_2.getString("c_state");
				c_zip = c_rs_2.getString("c_zip");
				c_phone = c_rs_2.getString("c_phone");
				c_credit = c_rs_2.getString("c_credit");
				c_credit_lim = c_rs_2.getInt("c_credit_lim");
				c_discount = c_rs_2.getInt("c_discount");
				c_balance = c_rs_2.getInt("c_balance");
				c_ytd_payment = c_rs_2.getInt("c_ytd_payment");
				c_payment_cnt = c_rs_2.getInt("c_payment_cnt");
				c_since = c_rs_2.getTimestamp("c_since");
				c_data = c_rs_2.getString("C_DATA");
				c_rs_2.close();

				// Update customers info
				PreparedStatement stmt_c_u = conn.prepareStatement("UPDATE " + "CUSTOMER" + "   SET C_BALANCE = ?, "
						+ "       C_YTD_PAYMENT = ?, " + "       C_PAYMENT_CNT = ?, " + "       C_DATA = ? "
						+ " WHERE C_W_ID = ? " + "   AND C_D_ID = ? " + "   AND C_ID = ?");
				stmt_c_u.setInt(1, c_balance + (-1 * paymentAmount));
				stmt_c_u.setInt(2, c_ytd_payment + paymentAmount);
				stmt_c_u.setInt(3, c_payment_cnt + 1);
				stmt_c_u.setString(4, "updated data");
				stmt_c_u.setInt(5, customerWarehouseID);
				stmt_c_u.setInt(6, customerDistrictID);
				stmt_c_u.setInt(7, c_id);
				stmt_c_u.executeUpdate();

			}

			//
			//
			// create H_DATA and insert a new row into HISTORY
			if (w_name.length() > 10)
				w_name = w_name.substring(0, 10);
			if (d_name.length() > 10)
				d_name = d_name.substring(0, 10);
			String h_data = w_name + "    " + d_name;
			PreparedStatement stmt_h = conn.prepareStatement("SELECT H_AMOUNT FROM HISTORY WHERE" + " H_C_D_ID=?"
					+ " AND H_C_W_ID=?" + " AND H_C_ID=?" + " AND H_D_ID=?" + " AND H_W_ID=?");
			stmt_h.setInt(1, customerDistrictID);
			stmt_h.setInt(2, customerWarehouseID);
			stmt_h.setInt(3, c_id);
			stmt_h.setInt(4, d_id);
			stmt_h.setInt(5, w_id);
			ResultSet h_rs = stmt_h.executeQuery();
			h_rs.next();
			int old_amount = h_rs.getInt("H_AMOUNT");

			PreparedStatement stmt_h_u = conn.prepareStatement("INSERT INTO " + "HISTORY"
					+ " (H_C_D_ID, H_C_W_ID, H_C_ID, H_D_ID, H_W_ID, H_DATE, H_AMOUNT, H_DATA) "
					+ " VALUES (?,?,?,?,?,?,?,?)");
			stmt_h_u.setInt(1, customerDistrictID);
			stmt_h_u.setInt(2, customerWarehouseID);
			stmt_h_u.setInt(3, c_id);
			stmt_h_u.setInt(4, d_id);
			stmt_h_u.setInt(5, w_id);
			stmt_h_u.setInt(6, current_time);
			stmt_h_u.setInt(7, old_amount + paymentAmount);
			stmt_h_u.setString(8, "new h_data");
			stmt_h_u.executeUpdate();

		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	public void stockLevel(int w_id, int d_id, int threshold, boolean _VERBOSE) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			conn = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			//
			// retrieve the latest order_id from the given district
			PreparedStatement stmt_d = conn.prepareStatement(
					"SELECT D_NEXT_O_ID " + "  FROM " + "DISTRICT" + " WHERE D_W_ID = ? " + "   AND D_ID = ?");
			stmt_d.setInt(1, w_id);
			stmt_d.setInt(2, d_id);
			ResultSet d_rs = stmt_d.executeQuery();
			if (!d_rs.next()) {
				System.out.println("ERROR_51: district does not exist: " + w_id + "," + d_id);
			}
			int o_id = d_rs.getInt("D_NEXT_O_ID");
			d_rs.close();
			// retrieve the latest 20 orders
			PreparedStatement stmt_ol = conn.prepareStatement(
					"select OL_I_ID from order_line WHERE OL_W_ID=? AND OL_D_ID=? AND OL_O_ID < ? AND OL_O_ID > ?");
			stmt_ol.setInt(1, w_id);
			stmt_ol.setInt(2, d_id);
			stmt_ol.setInt(3, o_id);
			stmt_ol.setInt(4, (o_id + (-1 * 20)));
			ResultSet ol_rs = stmt_ol.executeQuery();
			// ol_rs.next();
			while (ol_rs.next()) {
				int ol_i_id = ol_rs.getInt("ol_i_id");
				PreparedStatement stmt_s = conn.prepareStatement(
						"SELECT * FROM STOCK WHERE " + "s_w_id=? " + "AND S_QUANTITY < ? " + " AND s_i_id = ?");
				stmt_s.setInt(1, w_id);
				stmt_s.setInt(2, threshold);
				stmt_s.setInt(3, ol_i_id);
				ResultSet s_rs = stmt_s.executeQuery();
				if (_VERBOSE)
					while (s_rs.next()) {
						System.out.println("low stock found: " + s_rs.getInt("s_i_id"));
					}
			}

		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	public void orderStatus(int w_id, int d_id, int customerByName, int c_id, String c_last) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			conn = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			if (customerByName == 0) {
				PreparedStatement stmt_c = conn
						.prepareStatement("SELECT " + "C_ID, C_FIRST, C_MIDDLE, C_LAST, C_BALANCE" + "  FROM "
								+ "CUSTOMER" + " WHERE C_W_ID = ? " + "   AND C_D_ID = ? " + "   AND C_LAST = ? ");
				stmt_c.setInt(1, w_id);
				stmt_c.setInt(2, d_id);
				stmt_c.setString(3, c_last);
				ResultSet c_rs = stmt_c.executeQuery();
				c_rs.next();
				String c_first = c_rs.getString("c_first");
				String c_middle = c_rs.getString("c_middle");
				int c_balance = c_rs.getInt("c_balance");
				int new_c_id = c_rs.getInt("C_ID");

				c_rs.close();

				// find the newest order for the customer
				// retrieve the carrier & order date for the most recent order.
				PreparedStatement stmt_o = conn.prepareStatement("SELECT O_ID, O_CARRIER_ID, O_ENTRY_D " + "  FROM "
						+ "OORDER" + " WHERE O_W_ID = ? " + "   AND O_D_ID = ? " + "AND O_C_ID = ? ");
				stmt_o.setInt(1, w_id);
				stmt_o.setInt(2, d_id);
				stmt_o.setInt(3, new_c_id);
				ResultSet o_rs = stmt_o.executeQuery();
				o_rs.next();
				int o_id = o_rs.getInt("O_ID");
				int o_carrier_id = o_rs.getInt("O_CARRIER_ID");
				int o_entry_d = o_rs.getInt("O_ENTRY_D");
				o_rs.close();
				PreparedStatement stmt_ol = conn.prepareStatement(
						"SELECT OL_I_ID, OL_SUPPLY_W_ID, OL_QUANTITY, OL_AMOUNT, OL_DELIVERY_D " + "  FROM "
								+ "ORDER_LINE" + " WHERE OL_O_ID = ?" + "   AND OL_D_ID = ?" + "   AND OL_W_ID = ?");
				stmt_ol.setInt(1, o_id);
				stmt_ol.setInt(2, d_id);
				stmt_ol.setInt(3, w_id);
				ResultSet ol_rs_1 = stmt_ol.executeQuery();
				// craft the final result
				ArrayList<String> orderLines = new ArrayList<String>();
				while (ol_rs_1.next()) {
					StringBuilder sb = new StringBuilder();
					sb.append("[");
					sb.append(ol_rs_1.getInt("OL_SUPPLY_W_ID"));
					sb.append(" - ");
					sb.append(ol_rs_1.getInt("OL_I_ID"));
					sb.append(" - ");
					sb.append(ol_rs_1.getInt("OL_QUANTITY"));
					sb.append(" - ");
					sb.append(String.valueOf(ol_rs_1.getDouble("OL_AMOUNT")));
					sb.append(" - ");
					if (ol_rs_1.getInt("OL_DELIVERY_D") != -1)
						sb.append(ol_rs_1.getInt("OL_DELIVERY_D"));
					else
						sb.append("99-99-9999");
					sb.append("]");
					orderLines.add(sb.toString());
				}

			} else {
				PreparedStatement stmt_c2 = conn.prepareStatement("SELECT " + "C_FIRST, C_MIDDLE, C_LAST, C_BALANCE"
						+ "  FROM " + "CUSTOMER" + " WHERE C_W_ID = ? " + "   AND C_D_ID = ? " + "   AND C_LAST = ? ");
				stmt_c2.setInt(1, w_id);
				stmt_c2.setInt(2, d_id);
				stmt_c2.setString(3, c_last);
				ResultSet c_rs2 = stmt_c2.executeQuery();
				c_rs2.next();
				String c_first = c_rs2.getString("c_first");
				String c_middle = c_rs2.getString("c_middle");
				String new_c_last = c_rs2.getString("c_last");
				int c_balance = c_rs2.getInt("c_balance");
				c_rs2.close();

				// find the newest order for the customer
				// retrieve the carrier & order date for the most recent order.
				PreparedStatement stmt_o_2 = conn.prepareStatement("SELECT O_ID, O_CARRIER_ID, O_ENTRY_D " + "  FROM "
						+ "OORDER" + " WHERE O_W_ID = ? " + "   AND O_D_ID = ? " + "AND O_C_ID = ? ");
				stmt_o_2.setInt(1, w_id);
				stmt_o_2.setInt(2, d_id);
				stmt_o_2.setInt(3, c_id);
				ResultSet o_rs_2 = stmt_o_2.executeQuery();
				o_rs_2.next();
				int o_id = o_rs_2.getInt("O_ID");
				int o_carrier_id = o_rs_2.getInt("O_CARRIER_ID");
				int o_entry_d = o_rs_2.getInt("O_ENTRY_D");
				o_rs_2.close();
				PreparedStatement stmt_ol_2 = conn.prepareStatement(
						"SELECT OL_I_ID, OL_SUPPLY_W_ID, OL_QUANTITY, OL_AMOUNT, OL_DELIVERY_D " + "  FROM "
								+ "ORDER_LINE" + " WHERE OL_O_ID = ?" + "   AND OL_D_ID = ?" + "   AND OL_W_ID = ?");
				stmt_ol_2.setInt(1, o_id);
				stmt_ol_2.setInt(2, d_id);
				stmt_ol_2.setInt(3, w_id);
				ResultSet ol_rs_2 = stmt_ol_2.executeQuery();
				// craft the final result
				ArrayList<String> orderLines = new ArrayList<String>();
				while (ol_rs_2.next()) {
					StringBuilder sb = new StringBuilder();
					sb.append("[");
					sb.append(ol_rs_2.getInt("OL_SUPPLY_W_ID"));
					sb.append(" - ");
					sb.append(ol_rs_2.getInt("OL_I_ID"));
					sb.append(" - ");
					sb.append(ol_rs_2.getInt("OL_QUANTITY"));
					sb.append(" - ");
					sb.append(String.valueOf(ol_rs_2.getDouble("OL_AMOUNT")));
					sb.append(" - ");
					if (ol_rs_2.getInt("OL_DELIVERY_D") != -1)
						sb.append(ol_rs_2.getInt("OL_DELIVERY_D"));
					else
						sb.append("99-99-9999");
					sb.append("]");
					orderLines.add(sb.toString());
				}
			}

			// ❄❄❄❄❄❄❄❄❄❄❄❄❄❄❄
			// TXN SUCCESSFUL!
			// ❄❄❄❄❄❄❄❄❄❄❄❄❄❄❄

		} catch (Exception e) {
			throw e;
		} finally {
		}
	}

	public void delivery(int w_id, int o_carrier_id, int d_id, int currentTime) throws Exception {
		try {
			Class.forName("com.github.adejanovski.cassandra.jdbc.CassandraDriver");
			System.out.println("connecting...");
			conn = DriverManager.getConnection("jdbc:cassandra://localhost" + ":1904" + insID + "/testks");
			PreparedStatement stmt_d = conn.prepareStatement(
					"SELECT NO_O_ID FROM " + "NEW_ORDER" + " WHERE NO_D_ID = ? " + "   AND NO_W_ID = ? ");
			stmt_d.setInt(1, d_id);
			stmt_d.setInt(2, w_id);
			ResultSet no_rs = stmt_d.executeQuery();
			if (!no_rs.next()) {
				// This district has no new orders
				// This can happen but should be rare
				System.out.println(String.format("District has no new orders [W_ID=%d, D_ID=%d]", w_id, d_id));
			}
			int no_o_id = no_rs.getInt("NO_O_ID");
			no_rs.close();
			// retrieve order
			PreparedStatement stmt_O = conn.prepareStatement(
					"SELECT O_C_ID FROM " + "OORDER" + " WHERE O_ID = ? " + "   AND O_D_ID = ? " + "   AND O_W_ID = ?");
			stmt_O.setInt(1, no_o_id);
			stmt_O.setInt(2, d_id);
			stmt_O.setInt(3, w_id);
			ResultSet oo_rs = stmt_O.executeQuery();
			if (!oo_rs.next()) {
				System.out.println(String.format(
						"ERROR_41: Failed to retrieve ORDER record [W_ID=%d, D_ID=%d, O_ID=%d]", w_id, d_id, no_o_id));
			}
			int c_id = oo_rs.getInt("O_C_ID");
			oo_rs.close();

			// delete the row containing the oldest order
			PreparedStatement no_stmt = conn.prepareStatement(
					"DELETE FROM " + "NEW_ORDER" + " WHERE NO_O_ID = ? " + " AND NO_D_ID = ?" + "   AND NO_W_ID = ?");

			no_stmt.setInt(1, no_o_id);
			no_stmt.setInt(2, d_id);
			no_stmt.setInt(3, w_id);
			no_stmt.executeUpdate();

			//
			// update order's carrier id
			PreparedStatement oo_stmt = conn.prepareStatement("UPDATE OORDER  SET O_CARRIER_ID = ? "
					+ " WHERE O_ID = ? " + "   AND O_D_ID = ?" + "   AND O_W_ID = ?");
			oo_stmt.setInt(1, o_carrier_id);
			oo_stmt.setInt(2, no_o_id);
			oo_stmt.setInt(3, d_id);
			oo_stmt.setInt(4, w_id);
			oo_stmt.executeUpdate();

			// retrieve and update all orderlines belonging to this order
			PreparedStatement stmt_ol = conn.prepareStatement("SELECT OL_NUMBER, OL_AMOUNT FROM ORDER_LINE "
					+ " WHERE OL_O_ID = ? " + "   AND OL_D_ID = ? " + "   AND OL_W_ID = ? ");
			stmt_ol.setInt(1, no_o_id);
			stmt_ol.setInt(2, d_id);
			stmt_ol.setInt(3, w_id);
			ResultSet ol_rs = stmt_ol.executeQuery();
			// read all ol_numbers and get sum of ol_amounts
			double ol_total = 0;
			while (ol_rs.next()) {
				int ol_number = ol_rs.getInt("OL_NUMBER");
				ol_total += ol_rs.getDouble("OL_AMOUNT");
				PreparedStatement ol_stmt = conn.prepareStatement("UPDATE " + "ORDER_LINE" + "   SET OL_DELIVERY_D = ? "
						+ " WHERE OL_O_ID = ? " + "   AND OL_D_ID = ? " + "   AND OL_W_ID = ? " + "AND OL_NUMBER = ?");
				ol_stmt.setInt(1, currentTime);
				ol_stmt.setInt(2, no_o_id);
				ol_stmt.setInt(3, d_id);
				ol_stmt.setInt(4, w_id);
				ol_stmt.setInt(5, ol_number);
				ol_stmt.executeUpdate();
			}
			// retrieve customer's info
			PreparedStatement stmt_c = conn.prepareStatement("SELECT  C_BALANCE, C_DELIVERY_CNT" + " FROM CUSTOMER"
					+ " WHERE C_W_ID = ? " + "   AND C_D_ID = ? " + " AND C_ID = ? ");
			stmt_c.setInt(1, w_id);
			stmt_c.setInt(2, d_id);
			stmt_c.setInt(3, c_id);
			ResultSet c_rs = stmt_c.executeQuery();
			if (!c_rs.next()) {
				System.out.println("ERROR_42: customer does not exist: " + w_id + "," + d_id + "," + c_id);
			}
			int c_balance = c_rs.getInt("C_BALANCE");
			int c_delivery_cnt = c_rs.getInt("C_DELIVERY_CNT");
			c_rs.close();
			// update customer's info
			PreparedStatement cu_stmt = conn.prepareStatement("UPDATE " + "CUSTOMER" + " SET C_BALANCE = ?," + " C_DELIVERY_CNT = ? "
					+ " WHERE C_W_ID = ? " + "   AND C_D_ID = ? " + " AND C_ID = ? ");
			cu_stmt.setDouble(1, c_balance + ol_total);
			cu_stmt.setInt(2, c_delivery_cnt + 1);
			cu_stmt.setInt(3, w_id);
			cu_stmt.setInt(4, d_id);
			cu_stmt.setInt(5, c_id);
			cu_stmt.executeUpdate();

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
