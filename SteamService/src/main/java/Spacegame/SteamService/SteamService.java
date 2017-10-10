package Spacegame.SteamService;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import micronet.annotation.MessageListener;
import micronet.annotation.MessageService;
import micronet.network.Context;
import micronet.network.Request;
import micronet.network.Response;
import micronet.network.StatusCode;
import micronet.serialization.Serialization;

@MessageService(uri = "mn://steam")
public class SteamService {
	

	@MessageListener(uri = "/login")
	public Response steamLogin(Context context, Request request) {

		try {
			String url = "https://api.steampowered.com/ISteamUserAuth/AuthenticateUserTicket/v1/";
			String charset = StandardCharsets.UTF_8.name();
			
			String key = "C2DE20F15577E5D88FEBBE4B9524AFEC";
			String appid = "728690";
			String ticket = request.getData().replace("\"", "");
			
			String query = String.format("appid=%s&key=%s&ticket=%s", appid, key, ticket);

		
			URLConnection connection = new URL(url + "?" + query).openConnection();
			connection.setRequestProperty("Accept-Charset", charset);
			InputStream response = connection.getInputStream();

			
			try (Scanner scanner = new Scanner(response)) {
			    String responseBody = scanner.useDelimiter("\\A").next();

			    JsonObject steamLogin = new JsonParser().parse(responseBody).getAsJsonObject();
			    
			    JsonObject params = steamLogin.get("response")
			    		.getAsJsonObject().get("params")
			    		.getAsJsonObject();

			    String status = params.get("result").getAsString();
			    String steamID = params.get("steamid").getAsString();
			    
			    System.out.println("Steam Login:");
			    System.out.println(responseBody);
			    
			    if (status.toLowerCase().equals("ok"))
			    	return new Response(StatusCode.OK, steamID);
			}
		
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new Response(StatusCode.NOT_FOUND, "Steam Authentication failed!");
	}
}

