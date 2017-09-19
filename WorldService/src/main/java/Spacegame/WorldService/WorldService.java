package Spacegame.WorldService;

import java.net.URI;
import java.util.UUID;

import micronet.annotation.MessageListener;
import micronet.annotation.MessageService;
import micronet.annotation.OnStart;
import micronet.annotation.OnStop;
import micronet.network.Context;
import micronet.network.IAdvisory;
import micronet.network.Request;
import micronet.network.Response;
import micronet.network.StatusCode;
import micronet.script.ScriptExecutor;
import micronet.serialization.Serialization;

@MessageService(uri="mn://world")
public class WorldService {
	
	InstanceStore instanceStore;

	@OnStart
	public void onStart(Context context) {
		instanceStore = new InstanceStore();
		
		context.getAdvisory().registerConnectionStateListener((String id, IAdvisory.ConnectionState state) -> {
			if (state == IAdvisory.ConnectionState.DISCONNECTED) {
				String regionID = instanceStore.getRegionFromInstance(id);
				if (regionID != null) {
					int port = instanceStore.getInstancePort(regionID);
					instanceStore.removeInstance(regionID);
					context.sendRequest("mn://port/release", new Request(Integer.toString(port)));
				}
			}
		});
		context.getAdvisory().listen("User.Disconnected", advisory -> {
			int userID = Integer.parseInt(advisory);
			instanceStore.unqueueUser(userID);
		});
	}

	@OnStop
	public void onStop(Context context) {
	}
	
	@MessageListener(uri = "/join")
	public Response joinWorld(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		Response avatarResponse = context.sendRequestBlocking("mn://avatar/current/set", request);
		AvatarValues avatar = Serialization.deserialize(avatarResponse.getData(), AvatarValues.class);
		
		if (avatar.getRegionID() != null) {
			return joinWorld(context, userID, avatar.getRegionID(), avatarResponse.getData());
		}
		return joinWorld(context, userID, avatar.getHomeRegionID(), avatarResponse.getData());
	}
	
	@MessageListener(uri = "/travel/home")
	public Response homestone(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		Response avatarResponse = context.sendRequestBlocking("mn://avatar/current/get", request);
		AvatarValues avatar = Serialization.deserialize(avatarResponse.getData(), AvatarValues.class);
		return joinWorld(context, userID, avatar.getHomeRegionID(), avatarResponse.getData());
	}

	@MessageListener(uri = "/travel")
	public Response travel(Context context, Request request) {
		int userID = request.getParameters().getInt(ParameterCode.USER_ID);
		Response avatarResponse = context.sendRequestBlocking("mn://avatar/current/get", request);
		AvatarValues avatar = Serialization.deserialize(avatarResponse.getData(), AvatarValues.class);
		if (avatar.getLanded())
			return new Response(StatusCode.FORBIDDEN, "Must be in Space to Travel");
		return joinWorld(context, userID, request.getData(), avatarResponse.getData());
	}


	@MessageListener(uri = "/instance/open")
	public Response readyInstance(Context context, Request request) {

		String instanceID = request.getParameters().getString(ParameterCode.ID);
		String regionID = request.getParameters().getString(ParameterCode.REGION_ID);
		String host = request.getParameters().getString(ParameterCode.HOST);
		int port = request.getParameters().getInt(ParameterCode.PORT);
		
		if (!instanceStore.existsInstance(new String(regionID))) {
			context.sendRequest("mn://port/release", new Request(Integer.toString(port)));
			return new Response(StatusCode.RESET_CONTENT, "Instance not used any longer");
		}
				
		instanceStore.openInstance(new String(regionID), instanceID, host, port);

		//TODO: Notify Player that they can join
		for (int userID : instanceStore.getQueuedUsers(new String(regionID))) {
			
			Request avatarRequest = new Request();
			avatarRequest.getParameters().set(ParameterCode.USER_ID, userID);
			Response avatarResponse = context.sendRequestBlocking("mn://avatar/current/get", avatarRequest);
			
			Response joinRegionResponse = joinRegion(context, userID, new String(regionID), avatarResponse.getData());
			
			Request regionReadyRequest = new Request(avatarResponse.getData());
			regionReadyRequest.getParameters().set(ParameterCode.TOKEN, joinRegionResponse.getParameters().getString(ParameterCode.TOKEN));
			regionReadyRequest.getParameters().set(ParameterCode.REGION_ID, regionID);
			regionReadyRequest.getParameters().set(ParameterCode.HOST, host);
			regionReadyRequest.getParameters().set(ParameterCode.PORT, port);
			context.sendEvent(userID, "OnRegionReady", regionReadyRequest);
		}
		
		System.out.println("Instance Opened: " + regionID + " on: " + instanceID);
		return new Response(StatusCode.OK, "Instance Added");
	}


	@MessageListener(uri = "/instance/close")
	public Response resetInstance(Context context, Request request) {
		String regionID = new String(request.getParameters().getString(ParameterCode.REGION_ID));
		int port = request.getParameters().getInt(ParameterCode.PORT);
		
		instanceStore.removeInstance(regionID);
		context.sendRequest("mn://port/release", new Request(Integer.toString(port)));
		return new Response(StatusCode.OK, "Instance Closed");
	}

	private Response joinWorld(Context context, int userID, String regionID, String avatarData) {
		// RegionID id = new RegionID(124554051589L);
		// RegionValues region = new RegionValues(id);

		System.out.println("World Join: " + regionID);
		instanceStore.unqueueUser(userID);
		
		// Check if player is waiting for another instance

		if (instanceStore.isRegionOpen(regionID)) {
			instanceStore.queueUser(regionID, userID);
			return joinRegion(context, userID, regionID, avatarData);
		} else if (instanceStore.isRegionOpening(regionID)) {
			instanceStore.queueUser(regionID, userID);
		} else {
			Response regionResponse = context.sendRequestBlocking("mn://region/get", new Request(regionID.toString()));
			if (regionResponse.getStatus() != StatusCode.OK) 
				return new Response(StatusCode.NOT_FOUND, "Region unknown");
			
			instanceStore.addInstance(regionID);
			instanceStore.queueUser(regionID, userID);
			
			Response portResponse = context.sendRequestBlocking("mn://port/reserve", new Request());
			if (portResponse.getStatus() != StatusCode.OK)
				return new Response(StatusCode.NO_CONTENT, "No free Ports available");
			
			Request openRequest = new Request(regionResponse.getData());
			openRequest.getParameters().set(ParameterCode.REGION_ID, regionID);
			openRequest.getParameters().set(ParameterCode.PORT, portResponse.getData());
			context.sendRequest("mn://instance-open", openRequest, response-> { 
				System.err.println("No free Instances available");
			});
		}
		return new Response(StatusCode.TEMPORARY_REDIRECT, "Wait for Region to be loaded"); 
	}
	

	private Response joinRegion(Context context, int userID, String regionID, String avatarData) {
		// Generate PlayerToken (random UUIDs)
		UUID token = UUID.randomUUID();
		Request joinRequest = new Request(avatarData);
		joinRequest.getParameters().set(ParameterCode.USER_ID, userID);
		joinRequest.getParameters().set(ParameterCode.TOKEN, token.toString());

		Object address = ScriptExecutor.INSTANCE.invokeFunction("regionAddress", regionID) + "/join";
		System.out.println("SENDING JOIN: " + address);
		URI destination = URI.create(address.toString());
		Response joinResponse = context.sendRequestBlocking(destination.toString(), joinRequest);
		if (joinResponse.getStatus() != StatusCode.OK)
			return new Response(StatusCode.UNAUTHORIZED);

		Request setRegionRequest = new Request();
		setRegionRequest.getParameters().set(ParameterCode.USER_ID, userID);
		setRegionRequest.getParameters().set(ParameterCode.REGION_ID, regionID.toString());
		context.sendRequest("mn://avatar/current/update", setRegionRequest);

		Response response = new Response(StatusCode.OK, avatarData);
		response.getParameters().set(ParameterCode.REGION_ID, regionID.toString());
		response.getParameters().set(ParameterCode.TOKEN, token.toString());
		response.getParameters().set(ParameterCode.HOST, instanceStore.getInstanceHost(regionID));
		response.getParameters().set(ParameterCode.PORT, instanceStore.getInstancePort(regionID));

		return response;
	}
}
