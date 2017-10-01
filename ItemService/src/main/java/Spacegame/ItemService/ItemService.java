package Spacegame.ItemService;

import java.util.Collections;

import micronet.annotation.MessageListener;
import micronet.annotation.MessageService;
import micronet.datastore.DataStore;
import micronet.network.Context;
import micronet.network.Request;
import micronet.network.Response;
import micronet.network.StatusCode;
import micronet.serialization.Serialization;

@MessageService(uri="mn://item")
public class ItemService {

	DataStore store = new DataStore();

	@MessageListener(uri="/inventory/create")
	public void createInventory(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		String avatarName = request.getData();
		
		store.getSub(playerID).getSub("items").getMap("inventory").add(avatarName, Collections.nCopies(10, null));
	}
	
	@MessageListener(uri="/inventory/all")
	public Response getInventory(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		String avatarName = store.getSub(playerID).get("currentAvatar", String.class);

		String data = store.getSub(playerID).getSub("items").getMap("inventory").getRaw(avatarName);
		return new Response(StatusCode.OK, data);
	}

	@MessageListener(uri="/inventory/get")
	public Response getInventorItem(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		String avatarName = store.getSub(playerID).get("currentAvatar", String.class);
		int index = request.getParameters().getInt(ParameterCode.INDEX);

		String data = store.getSub(playerID).getSub("items").getMap("inventory").get(avatarName).asList().getRaw(index);
		return new Response(StatusCode.OK, data);
	}

	@MessageListener(uri = "/inventory/remove")
	public Response removeFromInventory(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		String avatarName = store.getSub(playerID).get("currentAvatar", String.class);
		int index = request.getParameters().getInt(ParameterCode.INDEX);

		store.getSub(playerID).getSub("items").getMap("inventory").get(avatarName).asList().set(index, null);
		sendInventoryChangedEvent(context, userID);
		return new Response(StatusCode.OK);
	}

	@MessageListener(uri = "/inventory/move")
	public Response moveInventoryItem(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		String avatarName = store.getSub(playerID).get("currentAvatar", String.class);
		Integer[] indices = Serialization.deserialize(request.getData(), Integer[].class);

		ItemValues item1 = store.getSub(playerID).getSub("items").getMap("inventory").get(avatarName).asList().get(indices[0], ItemValues.class);
		ItemValues item2 = store.getSub(playerID).getSub("items").getMap("inventory").get(avatarName).asList().get(indices[1], ItemValues.class);

		if (item2 == null) {
			store.getSub(playerID).getSub("items").getMap("inventory").get(avatarName).asList().set(indices[1], item1);
			store.getSub(playerID).getSub("items").getMap("inventory").get(avatarName).asList().set(indices[0], null);
		} else {
			store.getSub(playerID).getSub("items").getMap("inventory").get(avatarName).asList().set(indices[0], item2);
			store.getSub(playerID).getSub("items").getMap("inventory").get(avatarName).asList().set(indices[1], item1);
		}

		sendInventoryChangedEvent(context, userID);
		return new Response(StatusCode.OK);
	}

	@MessageListener(uri = "/inventory/add")
	public Response addInventoryItem(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		String avatarName = store.getSub(playerID).get("currentAvatar", String.class);
		
		ItemValues item = Serialization.deserialize(request.getData(), ItemValues.class);
		
		ItemValues[] inventory = store.getSub(playerID).getSub("items").getMap("inventory").get(avatarName, ItemValues[].class);

		for (int i = 0; i < inventory.length; i++) {
			if (inventory[i] == null) {
				inventory[i] = item;
				
				store.getSub(playerID).getSub("items").getMap("inventory").put(avatarName, inventory);
				sendInventoryChangedEvent(context, userID);
				return new Response(StatusCode.OK);
			}
		}
		
		return new Response(StatusCode.REQUESTED_RANGE_NOT_SATISFIABLE, "Inventory Full");
	}

	@MessageListener(uri = "/inventory/set")
	public Response setInventoryItem(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String playerID = String.format("Player.%s", userID);
		String avatarName = store.getSub(playerID).get("currentAvatar", String.class);
		int index = request.getParameters().getInt(ParameterCode.INDEX);

		String data = store.getSub(playerID).getSub("items").getMap("inventory").get(avatarName).asList().getRaw(index);
		
		if (data != null)
			return new Response(StatusCode.CONFLICT, "Target slot NOT empty");

		ItemValues item = Serialization.deserialize(request.getData(), ItemValues.class);
		store.getSub(playerID).getSub("items").getMap("inventory").get(avatarName).asList().set(index, item);

		sendInventoryChangedEvent(context, userID);
		return new Response(StatusCode.OK);
	}

	@MessageListener(uri = "/inventory/refresh")
	public Response refreshInventory(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		sendInventoryChangedEvent(context, userID);
		return new Response(StatusCode.OK);
	}

	private void sendInventoryChangedEvent(Context context, String userID) {
		String playerID = String.format("Player.%s", userID);
		String avatarName = store.getSub(playerID).get("currentAvatar", String.class);

		String data = store.getSub(playerID).getSub("items").getMap("inventory").getRaw(avatarName);
		context.sendEvent(userID, Event.InventoryChanged, new Request(data));
	}
}
