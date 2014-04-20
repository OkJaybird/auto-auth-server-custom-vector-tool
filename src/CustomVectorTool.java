import java.io.BufferedReader;
import java.io.Console;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import learning.FeatureVector;

import org.apache.http.auth.UsernamePasswordCredentials;

/*****************************************************************
	Jay Waldron
	jaywaldron@gmail.com
	Mar 21, 2014
 *****************************************************************/

public class CustomVectorTool {

	private static final String PROP_FILE = "database.properties";
	private static final String HOST_KEY = "hostname";
	private static final String PORT_KEY = "port";
	private static final String DB_KEY = "dbname";
	private static final String USER_KEY = "user";
	private static final String PASS_KEY = "pass";

	private static final String TABLE_NAME = "custom_vectors";
	private static final String CREATE_SQL = "create_table.sql";
	private static final String PORTAL_CONF = "portal.conf";

	public static void main(String[] args) {
		CustomVectorTool tool = new CustomVectorTool();
		Properties prop = tool.getPropertiesForDB(PROP_FILE);
		String url;

		if (prop == null) {
			System.out.println("Cannot load \""+PROP_FILE+"\" file. Ensure it exists in the same directory as this tool and its syntax is correct. " +
					"Keys must include: "+HOST_KEY+", "+PORT_KEY+", "+DB_KEY+", "+USER_KEY+", "+PASS_KEY+".");
			return;
		}
		url = tool.buildURL(prop);
		if (!tool.canConnectToDB(url, prop)) {
			System.out.println("Can't connect to DB. Check properties file for correctness and DB for proper permissions.");
			return;
		}
		if (!tool.tableExists(url, prop)) {
			if (!tool.createTable(url, prop)) {
				System.out.println("Unable to create table. Ensure correct syntax in "+CREATE_SQL+" and correct permissions in DB.");
				return;
			}
		}

		PageLoader loader = new PageLoader();
		if (!loader.addPortalFromFile(PORTAL_CONF)) {
			System.out.println("Unable to parse json file "+PORTAL_CONF+" for page access.");
			return;
		}


		UsernamePasswordCredentials creds = tool.getCredentials();
		System.out.println("Working...");

		FeatureVector vector = loader.viewPage(creds);
		if (vector == null) {
			System.out.println("HtmlUnit is having trouble navigating the page. This could be due to a variety of factors including" +
					" incorrectly handling unsupported technologies (such as Flash). If the site has a page mirror meant for mobile" +
					" browsers, try pointing the portal configuration file at such a page.");
		}

		System.out.println("Features gathered from Page:");
		for (Field f : vector.getClass().getFields()) {
			try {
				System.out.println(f.getName()+": "+f.get(vector));
			} catch (IllegalArgumentException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Wait for page snapshot to load, then classify to save.");
		System.out.println("Enter 't' for successful authentication. 'f' for unsuccessful. Enter anything else to avoid saving page info.");
		String input;
		boolean ans = false; boolean save = false;
		try{
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			if((input=br.readLine())!=null){
				if (input.equals("t")) {
					ans = true;
					save = true;
					System.out.println("Saving as True..");
				} else if (input.equals("f")) {
					ans = false;
					save = true;
					System.out.println("Saving as False..");
				} else {
					save = false;
					System.out.println("Throwing out page");
				}
			}
		}catch(IOException io){
			io.printStackTrace();
		}
		if (save) {
			vector.setClassification(ans);
			if (tool.saveVector(vector, loader.portalCode, url, prop)) {
				System.out.println("Successfully saved vector data");
			} else {
				System.out.println("Error saving vector info to DB.");
			}
		}
	}

	public Properties getPropertiesForDB(String filename) {
		Properties prop = new Properties();
		InputStream input = null;
		try {
			input = new FileInputStream(filename);
			prop.load(input);
			if (prop.containsKey(HOST_KEY) && prop.containsKey(PORT_KEY) && prop.containsKey(DB_KEY) && prop.containsKey(USER_KEY) && prop.containsKey(PASS_KEY)) {
				return prop;
			}
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	public String buildURL(Properties prop) {
		StringBuilder sb = new StringBuilder();
		if (prop.getProperty(HOST_KEY).endsWith("/")) {
			sb.append(prop.getProperty(HOST_KEY).substring(0, prop.getProperty(HOST_KEY).length()-1));
		} else {
			sb.append(prop.getProperty(HOST_KEY));
		}
		sb.append(":");
		sb.append(prop.getProperty(PORT_KEY));
		sb.append("/");
		sb.append(prop.getProperty(DB_KEY));
		return sb.toString();
	}

	public boolean canConnectToDB(String url, Properties prop) {
		Connection connection = null;
		try {
			connection = DriverManager.getConnection(url, prop.getProperty(USER_KEY), prop.getProperty(PASS_KEY));
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			if (connection != null) {
				try { connection.close(); } catch (SQLException e) {}
			}
		}
		return false;
	}

	public boolean tableExists(String url, Properties prop) {
		Connection conn = null;
		try {
			conn = DriverManager.getConnection (url, prop.getProperty(USER_KEY), prop.getProperty(PASS_KEY));
			DatabaseMetaData dbm = conn.getMetaData();
			ResultSet tables = dbm.getTables(null, null, TABLE_NAME, null);
			return tables.next();
		} catch (Exception e) {
			return false;
		} finally {
			if (conn != null) {
				try {
					conn.close ();
				} catch (Exception e) {}
			}
		}
	}

	public boolean createTable(String url, Properties prop) {
		Connection conn = null;
		Statement statement = null;
		try {
			List<String> lines = Files.readAllLines(Paths.get(CREATE_SQL), Charset.defaultCharset());
			StringBuilder sb = new StringBuilder();
			for (String line : lines) {
				sb.append(line);
				sb.append(" ");
			}
			String query = sb.toString();
			conn = DriverManager.getConnection (url, prop.getProperty(USER_KEY), prop.getProperty(PASS_KEY));
			statement = conn.createStatement();
			statement.executeUpdate(query);
			return true;
		} catch (Exception e) {
			return false;
		} finally {
			if (statement != null) {
				try {
					statement.close ();
				} catch (Exception e) {}
			}
			if (conn != null) {
				try {
					conn.close ();
				} catch (Exception e) {}
			}
		}
	}

	private boolean saveVector(FeatureVector featureVector, String portalCode, String url, Properties prop) {
		featureVector.portalCode = portalCode;
		Connection conn = null;
		PreparedStatement preparedStatement = null;
		try {
			Class.forName ("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(url, prop.getProperty(USER_KEY), prop.getProperty(PASS_KEY));
			String tableName = prop.getProperty(DB_KEY)+"."+TABLE_NAME;

			StringBuilder query = new StringBuilder();
			query.append("INSERT INTO "); query.append(tableName); query.append(" VALUES (");

			List<Object> rawValues = new ArrayList<Object>();
			for (Field f : featureVector.getClass().getFields()) {
				try {
					rawValues.add(f.get(featureVector));
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				}
			}
			for (@SuppressWarnings("unused") Object o : rawValues) {
				query.append(" ?,");
			}
			query.append(" DEFAULT);");
			preparedStatement = conn.prepareStatement(query.toString());

			int cursorPosition = 1;
			for (Object rawVal : rawValues) {
				if (rawVal instanceof String) {
					preparedStatement.setString(cursorPosition, (String)rawVal);
				} else if (rawVal instanceof Integer) {
					preparedStatement.setInt(cursorPosition, (int)rawVal);
				} else if (rawVal instanceof Double) {
					preparedStatement.setDouble(cursorPosition, (double)rawVal);
				}
				cursorPosition++;
			}
			preparedStatement.executeUpdate();
			return true;
		} catch (Exception e) {
			System.err.println(e);
			return false;
		} finally {
			if (conn != null) {
				try {
					preparedStatement.close();
					conn.close ();
				} catch (Exception e) {}
			}
		}
	}

	public UsernamePasswordCredentials getCredentials() {
		Console console = System.console();
		if (console == null) {
			System.out.println("Couldn't get Console instance");
			System.exit(0);
		}
		String username = console.readLine("Username: ");
		char passwordArray[] = console.readPassword("Password: ");
		return new UsernamePasswordCredentials(username,new String(passwordArray));
	}


}
