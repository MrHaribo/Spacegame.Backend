
var factionAttitude = function (confederateRep, rebelRep, friendlyP, hatefulP, neutral, confederate, rebel, outlaw) {
	
	if (confederateRep < hatefulP && rebelRep < hatefulP)
		return outlaw;
	if (confederateRep > friendlyP)
		return confederate;
	if (rebelRep > friendlyP)
		return rebel;
	return neutral;
	
}