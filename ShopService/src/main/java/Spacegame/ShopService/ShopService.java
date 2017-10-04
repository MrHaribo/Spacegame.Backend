package Spacegame.ShopService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

@MessageService(uri="mn://shop")
public class ShopService {
	
	private DataStore store = new DataStore();
	
	Map<String, ShopValues> shops = new HashMap<>();
	
	@OnStart
	public void onStart(Context context) {
		for (File cfgFile : new File("shops").listFiles()) {
			try {
				String data = new String(Files.readAllBytes(cfgFile.toPath()));
				ShopValues shop = Serialization.deserialize(data, ShopValues.class);
				shops.put(shop.getFaction(), shop);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	@MessageListener(uri = "/shops/add")
	public Response addShop(Context context, Request request) {
		String faction = request.getParameters().getString(ParameterCode.FACTION);
		ItemValues[] items = Serialization.deserialize(request.getData(), ItemValues[].class);
		String rankRestrictionsData = request.getParameters().getString(ParameterCode.INDEX);
		Integer[] rankRestrictions = Serialization.deserialize(rankRestrictionsData, Integer[].class);
		
		ShopValues shop = new ShopValues();
		shop.setFaction(faction);
		shop.setItems(Arrays.asList(items));
		shop.setRankRestriction(Arrays.asList(rankRestrictions));
		
		try {
			String data = Serialization.serializePretty(shop);
			Files.write(new File("shops/" + shop.getFaction()).toPath(), data.getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new Response(StatusCode.OK);
	}

	@MessageListener(uri = "/shops/get")
	public Response getShop(Context context, Request request) {
		String faction = request.getParameters().getString(ParameterCode.FACTION);
		return new Response(StatusCode.OK, Serialization.serialize(shops.get(faction).getItems()));
	}

	@MessageListener(uri = "/buy")
	public Response buy(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		ItemType type = Enum.valueOf(ItemType.class, request.getParameters().getString(ParameterCode.TYPE));
		Integer[] indices = Serialization.deserialize(request.getData(), Integer[].class);

		Request avatarRequest = new Request();
		avatarRequest.getParameters().set(ParameterCode.USER_ID, userID);
		Response avatarResponse = context.sendRequestBlocking("mn://avatar/current/get", avatarRequest);
		AvatarValues avatar = Serialization.deserialize(avatarResponse.getData(), AvatarValues.class);

		if (!avatar.getLanded())
			return new Response(StatusCode.FORBIDDEN, "Must be landed");
		
		RegionValues region = store.getSub(avatar.getRegionID()).get("data", RegionValues.class);
		
		
		Response factionResponse = context.sendRequestBlocking("mn://faction/reputation/get", avatarRequest);
		ReputationValues reputation = Serialization.deserialize(factionResponse.getData(), ReputationValues.class);
		
		Float rep = reputation.getReputation().get(region.getFaction());
		if (rep != null && rep < FactionValues.PercentageHostile) {
			return new Response(StatusCode.FORBIDDEN, "Buy Forbidden: Hostile Planet");
		}

		List<Integer> rankRestrictions = shops.get(region.getFaction()).getRankRestriction();
		int restriction = rankRestrictions.get(indices[0]);
		if (restriction > 0) {
			if (!avatar.getFaction().equals(region.getFaction()))
				return new Response(StatusCode.FORBIDDEN, "Buy Forbidden: You have the wrong Faction");
			
			int rank = 0;
			rank = rep >= FactionValues.PercentageRank1 ? 1 : rank;
			rank = rep >= FactionValues.PercentageRank2 ? 2 : rank;
			rank = rep >= FactionValues.PercentageRank3 ? 3 : rank;

			if (rank < restriction)
				return new Response(StatusCode.FORBIDDEN, "Insufficient Rank: Required " + restriction + " (Current " + rank + ")");
		}
		
		List<ItemValues> shopItems = shops.get(region.getFaction()).getItems();
		ItemValues itemToBuy = shopItems.get(indices[0]);

		// TODO: Start Transaction here. Is complicated because of Inventory
		if (itemToBuy.getPrice() > avatar.getCredits())
			return new Response(StatusCode.FORBIDDEN, "Insufficient Credits");

		System.out.println("Buy Request: " + itemToBuy.getName());

		Response inventoryResponse = null;
		if (type == ItemType.Vehicle) {
			inventoryResponse = buyVehicle(context, userID, itemToBuy);
		} else if (indices.length == 2) {
			inventoryResponse = buyItem(context, userID, itemToBuy, indices[1]);
		} else {
			inventoryResponse = buyItem(context, userID, itemToBuy);
		}

		if (inventoryResponse.getStatus() != StatusCode.OK)
			return inventoryResponse;

		Request creditsRequest = new Request(Integer.toString(itemToBuy.getPrice()));
		creditsRequest.getParameters().set(ParameterCode.USER_ID, userID);
		Response creditsResponse = context.sendRequestBlocking("mn://avatar/credits/remove", creditsRequest);

		// This should never happen
		if (creditsResponse.getStatus() != StatusCode.OK)
			return new Response(StatusCode.INTERNAL_SERVER_ERROR, "Money vanished during transaction");

		return new Response(StatusCode.OK);
	}

	Response buyVehicle(Context context, String userID, ItemValues item) {
		Request request = new Request(Serialization.serialize(item));
		request.getParameters().set(ParameterCode.USER_ID, userID);
		return context.sendRequestBlocking("mn://vehicle/add", request);
	}

	Response buyItem(Context context, String userID, ItemValues item) {
		Request request = new Request(Serialization.serialize(item));
		request.getParameters().set(ParameterCode.USER_ID, userID);
		return context.sendRequestBlocking("mn://item/inventory/add", request);
	}

	Response buyItem(Context context, String userID, ItemValues item, int inventoryIndex) {
		Request request = new Request(Serialization.serialize(item));
		request.getParameters().set(ParameterCode.USER_ID, userID);
		request.getParameters().set(ParameterCode.INDEX, inventoryIndex);
		return context.sendRequestBlocking("mn://item/inventory/set", request);
	}
}
