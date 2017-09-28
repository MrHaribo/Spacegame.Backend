package Spacegame.VehicleService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import micronet.annotation.MessageListener;
import micronet.annotation.MessageService;
import micronet.annotation.OnStart;
import micronet.annotation.OnStop;
import micronet.datastore.DataStore;
import micronet.network.Context;
import micronet.network.Request;
import micronet.network.Response;
import micronet.network.StatusCode;
import micronet.serialization.Serialization;

@MessageService(uri="mn://vehicle")
public class VehicleService {
	private VehicleDatabase database;
	private DataStore store = new DataStore();

	private Map<String, VehicleValues> vehicleConfigurations = new HashMap<>();
	private Map<String, VehicleValues> defaultVehicles = new HashMap<>();

	@OnStart
	public void onStart(Context context) {
		database = new VehicleDatabase();
		
		for (File cfgFile : new File("vehicle_configurations").listFiles()) {
			try {
				String data = new String(Files.readAllBytes(cfgFile.toPath()));
				VehicleValues vehicle = Serialization.deserialize(data, VehicleValues.class);
				vehicleConfigurations.put(vehicle.getName(), vehicle);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
				
		defaultVehicles.put("Rebel", vehicleConfigurations.get("Mice"));
		defaultVehicles.put("Confederate", vehicleConfigurations.get("Beetle"));
		defaultVehicles.put("Neutral", vehicleConfigurations.get("Drone"));
	}

	@OnStop
	public void onStop(Context context) {
		database.shutdown();
	}

	@MessageListener(uri = "/configuration/all/upload")
	public Response uploadAllConfigurations(Context context, Request request) {
		VehicleValues[] vehicles = Serialization.deserialize(request.getData(), VehicleValues[].class);
		//database.saveVehicleConfigurations(vehicles);
		
		for (VehicleValues vehicle : vehicles) {
			try {
				String data = Serialization.serializePretty(vehicle);
				Files.write(new File("vehicle_configurations/" + vehicle.getName()).toPath(), data.getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return new Response(StatusCode.OK);
	}

	@MessageListener(uri = "/change")
	public Response changeVehicle(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		Response avatarQuery = context.sendRequestBlocking("mn://avatar/current/get", request);
		AvatarValues avatar = Serialization.deserialize(avatarQuery.getData(), AvatarValues.class);

		if (!avatar.getLanded())
			return new Response(StatusCode.FORBIDDEN, "Must be landed to change Ship");

		int vehicleIndex = Integer.parseInt(request.getData());
		String vehicleCollectionID = String.format("Player.%s.vehicles", userID);
		store.getSub(vehicleCollectionID).getMap("currentVehicles").add(avatar.getName(), vehicleIndex);
		sendVehicleChangedEvent(context, userID, avatar.getName());
		return new Response(StatusCode.OK);
	}

	@MessageListener(uri = "/sell")
	public Response sellVehicle(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		Response avatarQuery = context.sendRequestBlocking("mn://avatar/current/name/get", request);
		String avatarName = avatarQuery.getData();

		String playerID = String.format("Player.%s", userID);
		int vehicleIndex = Integer.parseInt(request.getData());
		int currentVehicleIndex = store.getSub(playerID).getMap("vehicles.currentVehicles").get(avatarName, Integer.class);
		if (currentVehicleIndex == vehicleIndex)
			return new Response(StatusCode.FORBIDDEN, "Cannot Sell Ship in use");

		String vehicleListID = String.format("vehicles.vehicles.%s", avatarName);
		store.getSub(playerID).getList(vehicleListID).remove(vehicleIndex);

		if (vehicleIndex < currentVehicleIndex)
			store.getSub(playerID).getMap("vehicles.currentVehicles").add(avatarName, currentVehicleIndex - 1);

		// TODO: Sell Item via ShopService

		sendAvailableVehiclesChangedEvent(context, userID, avatarName);
		return new Response(StatusCode.OK);
	}

	@MessageListener(uri = "/current")
	public Response getCurrentVehicle(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		if (request.getParameters().containsParameter(ParameterCode.ID)) {
			userID = request.getParameters().getString(ParameterCode.ID);
		}
		String playerID = String.format("Player.%s", userID);
		String avatarName = context.sendRequestBlocking("mn://avatar/current/name/get", request).getData();

		int currentVehicleIndex = store.getSub(playerID).getMap("vehicles.currentVehicles").get(avatarName, Integer.class);
		String vehicleListID = String.format("vehicles.vehicles.%s", avatarName);
		VehicleValues vehicle = store.getSub(playerID).getList(vehicleListID).get(currentVehicleIndex, VehicleValues.class);
		if (vehicle == null)
			return new Response(StatusCode.FORBIDDEN, "You have no Ship");

		return new Response(StatusCode.OK, Serialization.serialize(vehicle));
	}

	@MessageListener(uri = "/add")
	public Response addVehicle(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		Response avatarQuery = context.sendRequestBlocking("mn://avatar/current/name/get", request);
		String avatarName = avatarQuery.getData();
		ItemValues vehicleItem = Serialization.deserialize(request.getData(), ItemValues.class);
		VehicleValues vehicle = vehicleConfigurations.get(vehicleItem.getName());
		
		String playerID = String.format("Player.%s", userID);
		String vehicleListID = String.format("vehicles.vehicles.%s", avatarName);
		store.getSub(playerID).getList(vehicleListID).append(vehicle);

		sendAvailableVehiclesChangedEvent(context, userID, avatarName);
		return new Response(StatusCode.OK);
	}

	@MessageListener(uri = "/available")
	public Response getAvailableVehicles(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		Response avatarQuery = context.sendRequestBlocking("mn://avatar/current/name/get", request);
		String avatarName = avatarQuery.getData();
		
		String playerID = String.format("Player.%s", userID);
		String vehicleListID = String.format("vehicles.vehicles.%s", avatarName);
		VehicleValues[] vehicles = store.getSub(playerID).get(vehicleListID, VehicleValues[].class);
		
		String data = Serialization.serialize(vehicles);
		Response response = new Response(StatusCode.OK, data);
		response.getParameters().set(ParameterCode.INDEX, database.getCurrentVehicleIndex(userID, avatarName));
		return response;
	}

	@MessageListener(uri = "/collection/remove")
	public void deleteVehicleCollection(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String avatarName = request.getData();
		
		String playerID = String.format("Player.%s", userID);
		store.getSub(playerID).getMap("vehicles.vehicles").remove(avatarName);
		store.getSub(playerID).getMap("vehicles.currentVehicles").remove(avatarName);
	}

	@MessageListener(uri = "/collection/create")
	public void createVehicleCollection(Context context, Request request) {
		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String faction = request.getParameters().getString(ParameterCode.FACTION);
		String avatarName = request.getData();
		
		String playerID = String.format("Player.%s", userID);
		VehicleCollection collection = store.getSub(playerID).get("vehicles", VehicleCollection.class);
		
		collection.getVehicles().put(avatarName, Arrays.asList(defaultVehicles.get(faction)));
		collection.getCurrentVehicles().put(avatarName, 0);
		store.getSub(playerID).set("vehicles", collection);
	}

	@MessageListener(uri = "/weapon/equip")
	public Response listenerName(Context context, Request request) {
		Integer[] indices = Serialization.deserialize(request.getData(), Integer[].class);

		Response avatarQuery = context.sendRequestBlocking("mn://avatar/current/get", request);
		AvatarValues avatar = Serialization.deserialize(avatarQuery.getData(), AvatarValues.class);

		if (!avatar.getLanded())
			return new Response(StatusCode.FORBIDDEN, "Weapons can only be changed when landed");

		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		ItemType weaponType = Enum.valueOf(ItemType.class, request.getParameters().getString(ParameterCode.ID));
		VehicleValues vehicle = database.getCurrentVehicle(userID, avatar.getName());

		Request itemRequest = new Request();
		itemRequest.getParameters().set(ParameterCode.USER_ID, userID);
		itemRequest.getParameters().set(ParameterCode.INDEX, indices[0]);
		Response getItemResponse = context.sendRequestBlocking("mn://item/inventory/get", itemRequest);
		ItemValues item = Serialization.deserialize(getItemResponse.getData(), ItemValues.class);

		if (weaponType != item.getType())
			return new Response(StatusCode.CONFLICT, "WeaponType Missmatch");

		switch (weaponType) {
		case PrimaryWeapon:
			if (vehicle.getPrimaryWeapons().get(indices[1]) != null)
				return new Response(StatusCode.CONFLICT, "Unequip Weapon first");
			vehicle.getPrimaryWeapons().set(indices[1], item.getName());
			break;
		case HeavyWeapon:
			if (vehicle.getHeavyWeapons().get(indices[1]) != null)
				return new Response(StatusCode.CONFLICT, "Unequip Weapon first");
			vehicle.getHeavyWeapons().set(indices[1], item.getName());
			break;
		default:
			break;
		}

		Response removeResponse = context.sendRequestBlocking("mn://item/inventory/remove", itemRequest);
		if (removeResponse.getStatus() != StatusCode.OK)
			return new Response(StatusCode.INTERNAL_SERVER_ERROR, "Weapon missing in inventory");

		int currentVehicleIndex = database.getCurrentVehicleIndex(userID, avatar.getName());
		database.updateVehicle(userID, avatar.getName(), currentVehicleIndex, vehicle);
		sendVehicleChangedEvent(context, userID, avatar.getName());
		return new Response(StatusCode.OK);
	}

	@MessageListener(uri = "/weapon/unequip")
	public Response unequipWeapon(Context context, Request request) {
		Integer[] indices = Serialization.deserialize(request.getData(), Integer[].class);

		Response avatarQuery = context.sendRequestBlocking("mn://avatar/current/get", request);
		AvatarValues avatar = Serialization.deserialize(avatarQuery.getData(), AvatarValues.class);

		if (!avatar.getLanded())
			return new Response(StatusCode.FORBIDDEN, "Weapons can only be changed when landed");

		String userID = request.getParameters().getString(ParameterCode.USER_ID);
		String weaponType = request.getParameters().getString(ParameterCode.ID);
		VehicleValues vehicle = database.getCurrentVehicle(userID, avatar.getName());

		ItemValues item = null;

		switch (weaponType) {
		case "PrimaryWeapon":
			item = new ItemValues(vehicle.getPrimaryWeapons().get(indices[0]), ItemType.PrimaryWeapon);
			break;
		case "HeavyWeapon":
			item = new ItemValues(vehicle.getHeavyWeapons().get(indices[0]), ItemType.HeavyWeapon);
			break;
		}

		Request addItemRequest = new Request(Serialization.serialize(item));
		addItemRequest.getParameters().set(ParameterCode.USER_ID, userID);
		addItemRequest.getParameters().set(ParameterCode.INDEX, indices[1]);
		Response addItemResponse = context.sendRequestBlocking("mn://item/inventory/set", addItemRequest);

		if (addItemResponse.getStatus() != StatusCode.OK)
			return new Response(StatusCode.FORBIDDEN, "Inventory Slot not empty");

		switch (weaponType) {
		case "PrimaryWeapon":
			vehicle.getPrimaryWeapons().set(indices[0], null);
			break;
		case "HeavyWeapon":
			vehicle.getHeavyWeapons().set(indices[0], null);
			break;
		}
		int currentVehicleIndex = database.getCurrentVehicleIndex(userID, avatar.getName());
		database.updateVehicle(userID, avatar.getName(), currentVehicleIndex, vehicle);
		sendVehicleChangedEvent(context, userID, avatar.getName());
		return new Response(StatusCode.OK);
	}

	private void sendVehicleChangedEvent(Context context, String userID, String avatarName) {
		VehicleValues vehicle = database.getCurrentVehicle(userID, avatarName);
		Request eventRequest = new Request(Serialization.serialize(vehicle));
		eventRequest.getParameters().set(ParameterCode.INDEX, database.getCurrentVehicleIndex(userID, avatarName));
		context.sendEvent(userID, "OnVehicleChanged", eventRequest);
	}

	private void sendAvailableVehiclesChangedEvent(Context context, String userID, String name) {
		// TODO: Don't serialize whole ship list, only delta
		VehicleValues[] vehicles = database.getVehicles(userID, name);
		Request eventRequest = new Request(Serialization.serialize(vehicles));
		eventRequest.getParameters().set(ParameterCode.INDEX, database.getCurrentVehicleIndex(userID, name));
		context.sendEvent(userID, "OnAvailableVehiclesChanged", eventRequest);
	}
}
