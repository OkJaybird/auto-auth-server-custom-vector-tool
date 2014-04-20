/*****************************************************************
	Jay Waldron
	jaywaldron@gmail.com
	Mar 21, 2014
 *****************************************************************/

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.logging.Level;

import learning.FeatureVector;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;

import org.apache.http.auth.UsernamePasswordCredentials;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;



public class PageLoader {

	private static boolean loggingDisabled = false;

	public String portalCode;
	private String url;
	private String firstClick;
	private String user;
	private String pass;
	private String button;

	public boolean addPortalFromFile(String filepath) {
		try {
			byte[] encoded = Files.readAllBytes(Paths.get(filepath));
			String jsonString = StandardCharsets.UTF_8.decode(ByteBuffer.wrap(encoded)).toString();
			JSONObject portal = (JSONObject) JSONValue.parse(jsonString);
			String portalCode = (String) portal.get("portal_code");
			String url = (String) portal.get("auth_url");
			String user = (String) portal.get("user_field_xpath");
			String pass = (String) portal.get("pass_field_xpath");
			String button = (String) portal.get("submit_button_xpath");
			String firstClick = "";
			if (portal.containsKey("first_click_xpath")) {
				firstClick = (String) portal.get("first_click_xpath");
			}
			this.portalCode = portalCode;
			this.url = url;
			this.firstClick = firstClick;
			this.user = user;
			this.pass = pass;
			this.button = button;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static void disableWebPageErrors() {
		if (!loggingDisabled) {
			java.util.logging.Logger.getLogger("com.gargoylesoftware").setLevel(Level.OFF);
			System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
			loggingDisabled = true;
		}
	}

	public FeatureVector viewPage(UsernamePasswordCredentials creds) {
		WebClient client = createNewWebClient();
		HtmlPage page = attemptLogin(creds, client);
		if (page == null) {
			return null;
		}

		try {
			// testing save this file for viewing in a browser
			File file = File.createTempFile(portalCode+"_response", ".html");
			file.delete();
			page.save(file);
			java.awt.Desktop.getDesktop().browse(file.toURI());
		} catch (Exception e) {e.printStackTrace(); System.out.println("couldn't open saved web page.");}

		return new FeatureVector(portalCode, false, page, client);
	}

	private WebClient createNewWebClient(){
		WebClient wc = new WebClient(BrowserVersion.CHROME_16);
		wc.getOptions().setRedirectEnabled(true);
		wc.getOptions().setJavaScriptEnabled(true);
		wc.getOptions().setPopupBlockerEnabled(false);
		wc.getOptions().setThrowExceptionOnFailingStatusCode(false);
		wc.getOptions().setThrowExceptionOnScriptError(false);
		wc.getOptions().setPrintContentOnFailingStatusCode(false);
		wc.getOptions().setUseInsecureSSL(true);
		wc.getOptions().setTimeout(30000);
		wc.getOptions().setActiveXNative(false);
		wc.getOptions().setAppletEnabled(false);
		wc.getOptions().setGeolocationEnabled(false);
		wc.getOptions().setCssEnabled(true);
		wc.setAjaxController(new NicelyResynchronizingAjaxController());
		disableWebPageErrors();
		return wc;
	}

	private HtmlPage attemptLogin(UsernamePasswordCredentials credentials, WebClient webclient) {
		HtmlPage authPage = null;
		try {
			authPage = webclient.getPage(url);
			if (!firstClick.equals("")) {
				HtmlElement e = authPage.getFirstByXPath(firstClick);
				int attempts = 0;
				while (e==null) {
					if (attempts >= 5) {
						return null;
					}
					attempts++;
					webclient.waitForBackgroundJavaScript(1000);
					e = authPage.getFirstByXPath(firstClick);
				}
				authPage = e.click();
			}
			if (authPage==null) {
				return null;
			}
		} catch (Exception e) {
		}

		HtmlInput userTextField = authPage.getFirstByXPath(user);
		HtmlInput passTextField = authPage.getFirstByXPath(pass);
		HtmlElement loginButton = authPage.getFirstByXPath(button);
		int attempts = 0;
		while (userTextField==null || passTextField==null || loginButton==null) {
			if (attempts >= 5) {
				return null;
			}
			attempts++;
			webclient.waitForBackgroundJavaScript(1000);
			userTextField = authPage.getFirstByXPath(user);
			passTextField = authPage.getFirstByXPath(pass);
			loginButton = authPage.getFirstByXPath(button);
		}
		userTextField.setValueAttribute(credentials.getUserName());
		passTextField.setValueAttribute(credentials.getPassword());
		HtmlPage responsePage = null;
		try {
			Page page = loginButton.click();
			webclient.waitForBackgroundJavaScript(3000);
			if (page instanceof HtmlPage) {
				responsePage = (HtmlPage)page;
			}
		} catch (IOException e) {
		}
		return responsePage;
	}

}