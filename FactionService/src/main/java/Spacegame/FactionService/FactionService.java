package Spacegame.FactionService;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import micronet.annotation.MessageListener;
import micronet.annotation.MessageService;
import micronet.annotation.OnStart;
import micronet.datastore.DataStore;
import micronet.network.Context;
import micronet.network.Request;
import micronet.network.Response;
import micronet.network.StatusCode;
import micronet.serialization.Serialization;

@MessageService(uri = "mn://faction")
public class FactionService {
	
	private final float reputationIncrement = 0.1f;
	private final float reputationDecrement = 0.2f;

	private final float initialFriendlyReputation = 0.3f;
	private final float initialHostileReputation = -0.2f;
	
	DataStore store = new DataStore();
	
	@OnStart
	public void onStart(Context context) {
		FactionValues faction = new FactionValues(FactionValues.Rebel);
		faction.setHostileFactions(new HashSet<>(Arrays.asList(FactionValues.Confederate)));
		faction.setAlliedFactions(new HashSet<>(Arrays.asList(FactionValues.Rebel)));
		String id = String.format("Faction.%s", FactionValues.Rebel);
		store.upsert(id, faction);
		
		faction = new FactionValues(FactionValues.Confederate);
		faction.setHostileFactions(new HashSet<>(Arrays.asList(FactionValues.Rebel)));
		faction.setAlliedFactions(new HashSet<>(Arrays.asList(FactionValues.Confederate)));		
		id = String.format("Faction.%s", FactionValues.Confederate);
		store.upsert(id, faction);
	}
	
	@MessageListener(uri = "/reputation/create")
	public void createReputation(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		String faction = request.getParameters().getString(ParameterCode.FACTION);
		String avatarName = request.getData();
		
		ReputationValues rep = new ReputationValues();
		rep.setReputation(new HashMap<>());
		
		switch (faction) {
		case FactionValues.Rebel:
			rep.getReputation().put(FactionValues.Rebel, 0.3f);
			rep.getReputation().put(FactionValues.Confederate, -0.5f);
			break;
		case FactionValues.Confederate:
			rep.getReputation().put(FactionValues.Rebel, -0.5f);
			rep.getReputation().put(FactionValues.Confederate, 0.3f);
			break;
			default:
		}
		
		store.getSub(playerID).getMap("reputation").put(avatarName, rep);
	}
	
	@MessageListener(uri = "/reputation/get")
	public Response getReputation(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		ReputationValues rep = getReputation(context, userID);
		return new Response(StatusCode.OK, Serialization.serialize(rep));
	}
	
	@MessageListener(uri = "/reputation/add")
	public void addReputation(Context context, Request request) {
	}
	
	@MessageListener(uri = "/reputation/remove")
	public void removeReputation(Context context, Request request) {

	}
	
	@MessageListener(uri = "/kill")
	public void onKill(Context context, Request request) {
		String killedFactionName = request.getParameters().getString(ParameterCode.FACTION);
		String[] involvedPlayers = Serialization.deserialize(request.getData(), String[].class);

		String killedFactionID = String.format("Faction.%s", killedFactionName);
		FactionValues killedFaction = store.get(killedFactionID , FactionValues.class);
		
		
		//Map<String, FactionValues> allFactions = getAllFactions(context);
		
		for (String involvedPlayerUserID : involvedPlayers) {

			Request avatarRequest = new Request();
			avatarRequest.getParameters().set(ParameterCode.USER_ID, involvedPlayerUserID);
			Response avatarResponse = context.sendRequestBlocking("mn://avatar/current/get", avatarRequest);
			AvatarValues avatar = Serialization.deserialize(avatarResponse.getData(), AvatarValues.class);
			
			String involvedPlayerID = String.format("Player.%s", involvedPlayerUserID);
			ReputationValues involvedPlayerReputation = store.getSub(involvedPlayerID).getMap("reputation").get(avatar.getName(), ReputationValues.class);
		
			if (killedFactionName.equals(FactionValues.Outlaw)) {
				for (Map.Entry<String,Float> rep : involvedPlayerReputation.getReputation().entrySet()) {
					if (rep.getValue() < 0) {
						involvedPlayerReputation.getReputation().put(rep.getKey(), rep.getValue() + reputationIncrement);
					}
				}
			} else if (killedFactionName.equals(FactionValues.Neutral)) {
				for (Map.Entry<String,Float> rep : involvedPlayerReputation.getReputation().entrySet()) {
					if (rep.getValue() > -1) {
						involvedPlayerReputation.getReputation().put(rep.getKey(), rep.getValue() - reputationDecrement * 2);
					}
				}
			} else {
				for (String alliedFaction : killedFaction.getAlliedFactions()) {
					if (involvedPlayerReputation.getReputation().containsKey(alliedFaction)) {
						float rep = involvedPlayerReputation.getReputation().get(alliedFaction) - reputationDecrement * 2;
						involvedPlayerReputation.getReputation().put(alliedFaction, rep < -1 ? -1 : rep);
					} else {
						involvedPlayerReputation.getReputation().put(alliedFaction, 0 - reputationDecrement * 2);
					}
				}
				for (String hostileFaction : killedFaction.getHostileFactions()) {
					if (involvedPlayerReputation.getReputation().containsKey(hostileFaction)) {
						float rep = involvedPlayerReputation.getReputation().get(hostileFaction) + reputationIncrement * 2;
						involvedPlayerReputation.getReputation().put(hostileFaction, rep > 1 ? 1 : rep);
					} else {
						involvedPlayerReputation.getReputation().put(hostileFaction, 0 + reputationIncrement * 2);
					}
				}
			}
			
			//TODO: Check for new Faction
			String oldFaction = avatar.getFaction();
			String newFaction = FactionValues.Outlaw;
			
			for (Map.Entry<String, Float> rep : involvedPlayerReputation.getReputation().entrySet()) {
				if (rep.getValue() > 0 && newFaction.equals(FactionValues.Outlaw))
					newFaction = FactionValues.Neutral;
				if (rep.getValue() > FactionValues.PercentageRank1)
					newFaction = rep.getKey();
			}
			

			if (!newFaction.equals(oldFaction)) {
				Request persistRequest = new Request();
				persistRequest.getParameters().set(ParameterCode.USER_ID, involvedPlayerUserID);
				persistRequest.getParameters().set(ParameterCode.NAME, avatar.getName());
				persistRequest.getParameters().set(ParameterCode.FACTION, newFaction);
				context.sendRequest("mn://avatar/persist", persistRequest);
			}
			

			store.getSub(involvedPlayerID).getMap("reputation").put(avatar.getName(), involvedPlayerReputation);
			sendReputationChangedEvent(context, involvedPlayerUserID);
		}
		
	}
	
	@MessageListener(uri = "/all")
	public Response getAllFactions(Context context, Request request) {
		Map<String, FactionValues> allFactions = getAllFactions(context);
		return new Response(StatusCode.OK, Serialization.serialize(allFactions));
	}
	
	private Map<String, FactionValues> getAllFactions(Context context) {
		String[] allFactionNames = store.getSub("World").get("factions", String[].class);
		Map<String, FactionValues> allFactions = new HashMap<>();
		
		for (String factionName : allFactionNames) {
			String factionID = String.format("Faction.%s", factionName);
			FactionValues faction = store.get(factionID, FactionValues.class);
			allFactions.put(factionName, faction);
		}
		return allFactions;
	}
	
	
	@MessageListener(uri = "/get")
	public Response getFaction(Context context, Request request) {
		String id = String.format("Faction.%s", request.getData());
		FactionValues faction = store.get(id, FactionValues.class);
		return new Response(StatusCode.OK, Serialization.serialize(faction));
	}
	
	@MessageListener(uri = "/change")
	public void changeFaction(Context context, Request request) {

	}
	
	private void sendReputationChangedEvent(Context context, String userID) {
		ReputationValues rep = getReputation(context, userID);
		context.sendEvent(userID, Event.ReputationChanged, Serialization.serialize(rep));
	}
	
	private ReputationValues getReputation(Context context, String userID) {
		String playerID = String.format("Player.%s", userID);
		String avatarName = store.getSub(playerID).get("currentAvatar", String.class);
		if (avatarName == null)
			return null;
		return store.getSub(playerID).getMap("reputation").get(avatarName, ReputationValues.class);
	}
}

