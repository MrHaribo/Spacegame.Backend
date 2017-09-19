package Spacegame.AvatarService;

import java.util.HashMap;
import java.util.Map;

import micronet.annotation.MessageListener;
import micronet.annotation.MessageService;
import micronet.annotation.OnStart;
import micronet.annotation.OnStop;
import micronet.network.Context;
import micronet.network.Request;
import micronet.network.Response;
import micronet.network.StatusCode;
import micronet.script.ScriptExecutor;
import micronet.serialization.Serialization;

@MessageService(uri="mn://avatar")  
public class AvatarService {

	private AvatarDatabase database; 

	// TODO: Avatars could be cached
	private Map<Integer, String> avatars = new HashMap<>();
	
	public static void main(String[] args) {

		Object invokeFunction = ScriptExecutor.INSTANCE.invokeFunction("testScript");
		System.out.println(invokeFunction);

		invokeFunction = ScriptExecutor.INSTANCE.invokeFunction("myRnd");
		System.out.println(invokeFunction);
		invokeFunction = ScriptExecutor.INSTANCE.invokeFunction("myRnd");
		System.out.println(invokeFunction);
		invokeFunction = ScriptExecutor.INSTANCE.invokeFunction("myRnd");
		System.out.println(invokeFunction);
		invokeFunction = ScriptExecutor.INSTANCE.invokeFunction("myRnd");
		System.out.println(invokeFunction);
		
	}
	
	@OnStart
	public void onStart(Context context) {
		database = new AvatarDatabase();
		
		context.getAdvisory().listen("User.Disconnected", (String advisory) -> {
			int userID = Integer.parseInt(advisory);
			avatars.remove(userID);
			System.out.println("User Disconnected" + advisory + " player left: " + avatars.size());
		});
	}
	
	@OnStop
	public void onStop(Context context) { 
		database.shutdown();
	}
	
	@MessageListener(uri="/credits/balance")
	public Response balanceCredits(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		AvatarValues avatar = getCurrentAvatar(userID);
		return new Response(StatusCode.OK, Integer.toString(avatar.getCredits()));
	}
	
	@MessageListener(uri="/credits/add")
	public Response addCredits(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		AvatarValues avatar = getCurrentAvatar(userID);
		int amount = Integer.parseInt(request.getData());
		int balance = avatar.getCredits() + amount;

		// TODO: Not Acomic Yet because of additional effort for real
		// transactions
		database.updateAvatar(userID, avatar.getName(), (AvatarValues tmp) -> {
			tmp.setCredits(balance);
			return tmp;
		});
		context.sendEvent(userID, "OnCreditsChanged", Integer.toString(balance));
		return new Response(StatusCode.OK, Integer.toString(balance));
	}
	
	@MessageListener(uri="/credits/remove")
	public Response removeCredits(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		AvatarValues avatar = getCurrentAvatar(userID);

		int amount = Integer.parseInt(request.getData());
		if (avatar.getCredits() >= amount) {
			int balance = avatar.getCredits() - amount;
			database.updateAvatar(userID, avatar.getName(), (AvatarValues tmp) -> {
				tmp.setCredits(balance);
				return tmp;
			});
			context.sendEvent(userID, "OnCreditsChanged", Integer.toString(balance));
			return new Response(StatusCode.OK, Integer.toString(balance));
		} else {
			return new Response(StatusCode.NOT_ACCEPTABLE, "Insufficient Credits");
		}
	}

	@MessageListener(uri="/reputation/add")
	public void addReputation(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		String attitude = request.getParameters().getString(ParameterCode.FACTION);
		float amount = Float.parseFloat(request.getData());

		if (!avatars.containsKey(userID))
			return;
		String avatarName = avatars.get(userID);

		database.updateAvatar(userID, avatarName, (AvatarValues tmp) -> {
			if (attitude.equals(FactionValues.AttitudeConfederate))
				tmp.getFaction().setConfederateReputation(tmp.getFaction().getConfederateReputation() + amount);
			if (attitude.equals(FactionValues.AttitudeRebel))
				tmp.getFaction().setRebelReputation(tmp.getFaction().getRebelReputation() + amount);
			return tmp;
		});
		
		sendReputationChangedEvent(context, userID, avatarName);
	}
	
	@MessageListener(uri="/land")
	public Response land(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		AvatarValues avatar = getCurrentAvatar(userID);

		if (avatar.getLanded())
			return new Response(StatusCode.BAD_REQUEST, "Already Landed");

		
		String regionId = avatar.getRegionID();
		String queue = ScriptExecutor.INSTANCE.invokeFunction("regionAddress", regionId).toString() + "/land";
		System.out.println("SENDING LAND: " + queue);
		Response landResponse = context.sendRequestBlocking(queue, request);

		if (landResponse.getStatus() != StatusCode.OK)
			return landResponse;

		database.updateAvatar(userID, avatar.getName(), (AvatarValues tmp) -> {
			tmp.setPosition(landResponse.getParameters().getVector2(ParameterCode.POSITION));
			tmp.setPoiID(landResponse.getParameters().getString(ParameterCode.POI_ID));
			tmp.setLanded(true);
			return tmp;
		});

		System.out.println(avatar.getName() + " lands on " + landResponse.getParameters().getString(ParameterCode.POI_ID));
		return new Response(StatusCode.OK);
	}

	@MessageListener(uri="/takeoff")
	public Response takeoff(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		AvatarValues avatar = getCurrentAvatar(userID);

		if (!avatar.getLanded())
			return new Response(StatusCode.BAD_REQUEST, "Already InSpace");

		String regionId = avatar.getRegionID();
		String queue = ScriptExecutor.INSTANCE.invokeFunction("regionAddress", regionId).toString() + "/takeoff";
		System.out.println("SENDING TAKEOFF: " + queue);
		Response takeoffResponse = context.sendRequestBlocking(queue, request);

		if (takeoffResponse.getStatus() != StatusCode.OK)
			return takeoffResponse;

		database.updateAvatar(userID, avatar.getName(), (AvatarValues tmp) -> {
			tmp.setLanded(false);
			return tmp;
		});

		System.out.println(avatar.getName() + " takeoff ");
		return new Response(StatusCode.OK);
	}


	@MessageListener(uri="/current/name/get")
	public Response getCurrentAvatarName(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		String avatarName = avatars.get(userID);
		return new Response(StatusCode.OK, avatarName);
	}
	
	@MessageListener(uri="/current/get")
	public Response getCurrentAvatar(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		AvatarValues avatar = getCurrentAvatar(userID);
		return new Response(StatusCode.OK, Serialization.serialize(avatar));
	}

	@MessageListener(uri="/current/set")
	public Response setCurrentAvatar(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		AvatarValues avatar = database.getAvatar(userID, request.getData());
		avatars.put(userID, avatar.getName());
		return new Response(StatusCode.OK, Serialization.serialize(avatar));
	}

	@MessageListener(uri="/get")
	public Response setAvatar(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		AvatarValues avatar = database.getAvatar(userID, request.getData());
		return new Response(StatusCode.OK, Serialization.serialize(avatar));
	}
	
	@MessageListener(uri="/all")
	public Response getAllAvatars(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		AvatarValues[] avatars = database.getAvatars(userID);
		if (avatars == null)
			avatars = new AvatarValues[0];
		String data = Serialization.serialize(avatars);
		return new Response(StatusCode.OK, data);
	}

	@MessageListener(uri="/create")
	public Response createAvatar(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		AvatarValues avatar = Serialization.deserialize(request.getData(), AvatarValues.class);

		if (avatar.getName() == null || avatar.getName().length() < 2)
			return new Response(StatusCode.BAD_REQUEST);
		avatar.setCredits(1000);

		String faction = request.getParameters().getString(ParameterCode.FACTION);
		if (faction.equals(FactionValues.AttitudeRebel)) {
			avatar.setFaction(new FactionValues(-0.3f, 0.4f));
			avatar.setRegionID("Region.32");
			avatar.setHomeRegionID("Region.32");
		} else if (faction.equals(FactionValues.AttitudeConfederate)) {
			avatar.setFaction(new FactionValues(0.4f, -0.3f));
			avatar.setRegionID("Region.33");
			avatar.setHomeRegionID("Region.33");
		} else if (faction.equals(FactionValues.AttitudeNeutral)) {
			avatar.setFaction(new FactionValues(0, 0));
			avatar.setRegionID("Region.1");
			avatar.setHomeRegionID("Region.1");
		}

		database.addAvatar(userID, avatar);
		sendAvailableAvatarsChangedEvent(context, userID);

		Request createCollectionRequest = new Request(avatar.getName());
		createCollectionRequest.getParameters().set(ParameterCode.USER_ID, userID);
		createCollectionRequest.getParameters().set(ParameterCode.FACTION, faction);
		context.sendRequest("mn://vehicle/collection/create", createCollectionRequest);

		return new Response(StatusCode.OK);
	}
	
	@MessageListener(uri="/delete")
	public Response deleteAvatar(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		database.deleteAvatar(userID, request.getData());
		sendAvailableAvatarsChangedEvent(context, userID);
		context.sendRequest("mn://vehicle/collection/remove", request);

		return new Response(StatusCode.OK);
	}

	@MessageListener(uri="/current/update")
	public void updateAvatar(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);

		if (!avatars.containsKey(userID))
			return;
		String avatarName = avatars.get(userID);

		database.updateAvatar(userID, avatarName, (AvatarValues avatar) -> {
			if (request.getParameters().containsParameter(ParameterCode.REGION_ID))
				avatar.setRegionID(request.getParameters().getString(ParameterCode.REGION_ID));
			if (request.getParameters().containsParameter(ParameterCode.POSITION))
				avatar.setPosition(request.getParameters().getVector2(ParameterCode.POSITION));
			return avatar;
		});
	}
	
	@MessageListener(uri="/persist")
	public void persistAvatar(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		String avatarName = request.getParameters().getString(ParameterCode.NAME);

		database.updateAvatar(userID, avatarName, (AvatarValues avatar) -> {
			if (request.getParameters().containsParameter(ParameterCode.REGION_ID))
				avatar.setRegionID(request.getParameters().getString(ParameterCode.REGION_ID));
			if (request.getParameters().containsParameter(ParameterCode.POSITION))
				avatar.setPosition(request.getParameters().getVector2(ParameterCode.POSITION));
			return avatar;
		});
	}
	
	private AvatarValues getCurrentAvatar(int userID) {
		String avatarName = avatars.get(userID);
		return database.getAvatar(userID, avatarName);
	}

	private void sendAvailableAvatarsChangedEvent(Context context, int userID) {
		AvatarValues[] avatars = database.getAvatars(userID);
		String data = Serialization.serialize(avatars);
		context.sendEvent(userID, "OnAvailableAvatarsChanged", data);
	}
	
	private void sendReputationChangedEvent(Context context, int userID, String avatarName) {
		AvatarValues avatar = database.getAvatar(userID, avatarName);
		String data = Serialization.serialize(avatar.getFaction());
		context.sendEvent(userID, "OnReputationChanged", data);
	}
}
