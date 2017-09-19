var regionPosition = function(seed, worldX, worldY) {
	Math.seedrandom(seed);

	var posX = worldX + Math.random() * 2 - 1;
	var posY = worldY + Math.random() * 2 - 1;
	
	var result = "";
	return result.concat("{", posX, ",", posY, "}");
}