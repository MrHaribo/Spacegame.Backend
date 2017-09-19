package Spacegame.RegionService;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import micronet.annotation.MessageListener;
import micronet.annotation.MessageService;
import micronet.annotation.OnStart;
import micronet.annotation.OnStop;
import micronet.network.Context;
import micronet.network.IAdvisory.QueueState;
import micronet.script.ScriptExecutor;
import micronet.network.Request;
import micronet.network.Response;
import micronet.network.StatusCode;
import micronet.serialization.Serialization;

@MessageService(uri="mn://region")
public class RegionService {

	private static final String[] confederateBattleRegions = { "Region.37", "Region.38", };
	private static final String[] rebelBattleRegions = { "Region.31", "Region.34", };
	
	int battleRegionCount = 42;
	Set<RegionValues> battleRegions = Collections.synchronizedSet(new HashSet<>());
	
	private RegionDatabase database;

	@OnStart
	public void onStart(Context context) {
		
		database = new RegionDatabase();
		if (System.getenv("region_pool_enabled") != null)
			new RegionPool(context.getAdvisory());
		
		//TODO: Make scalable with Couchbase
		CreateBattleRegions(context);
	}
	
	@OnStop
	public void onStop(Context context) {
		database.shutdown();
	}

	// This at the moment only returns the master region because region
	// generation does not work in java yet
	@MessageListener(uri = "/get")
	public Response getRegion(Context context, Request request) {
		String regionID = request.getData();

		RegionValues region = database.getMasterRegion(regionID);
		if (region == null)
			return new Response(StatusCode.NOT_IMPLEMENTED, "Master Region not found");

		region.setId(regionID);
		Response regionResponse = new Response(StatusCode.OK, Serialization.serialize(region));
		return regionResponse;
	}

	@MessageListener(uri = "/all")
	public Response getAllRegions(Context context, Request request) {
		List<String> regions = database.getAllMasterRegionsRaw();
		String data = Serialization.serialize(regions);
		return new Response(StatusCode.OK, data);
	}

	@MessageListener(uri = "/add")
	public Response addRegion(Context context, Request request) {
		RegionValues region = Serialization.deserialize(request.getData(), RegionValues.class);
		database.addMasterRegion(region);
		return new Response(StatusCode.OK);
	}
	
	@MessageListener(uri = "/battles/all")
	public Response getAllBattles(Context context, Request request) {
		return new Response(StatusCode.OK, Serialization.serialize(battleRegions));
	}
	
	private void CreateBattleRegions(Context context) {
		battleRegions = Collections.synchronizedSet(new HashSet<>());
		CreateBattleRegion(context, MatchType.Deathmatch, confederateBattleRegions[0]);
		CreateBattleRegion(context, MatchType.Deathmatch, confederateBattleRegions[1]);
		CreateBattleRegion(context, MatchType.Deathmatch, rebelBattleRegions[0]);
		CreateBattleRegion(context, MatchType.Deathmatch, rebelBattleRegions[1]);
	}
	
	private void CreateBattleRegion(Context context, MatchType type, String masterBattleRegionID) {
		String battleRegionID = "Battle.Region." + masterBattleRegionID  + "." + battleRegionCount++;
		CreateBattleRegion(context, battleRegionID);
	}

	private void CreateBattleRegion(Context context, String battleRegionID) {
		Response getRegionResponse = getRegion(context, new Request(battleRegionID.toString()));
		RegionValues battleRegion = Serialization.deserialize(getRegionResponse.getData(), RegionValues.class);
		battleRegions.add(battleRegion);
		
		context.broadcastEvent("OnBattleRegionsChanged", Serialization.serialize(battleRegions));
		
		Object id = ScriptExecutor.INSTANCE.invokeFunction("regionAddress", battleRegionID);
		context.getAdvisory().registerQueueStateListener(id.toString(), (QueueState state) -> {
			if (state == QueueState.CLOSE) {
				battleRegions.remove(battleRegion);
				context.getAdvisory().unregisterQueueStateListener(id.toString());
				//CreateBattleRegion(context, id.toString(), confederateBattleRegions[0]);
			}
		});
	}
}
