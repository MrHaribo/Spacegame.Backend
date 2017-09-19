var factionHostile = function (neutral, confederate, rebel, outlaw, attitude1, attitude2) {
	
    switch (attitude1)
    {
        case outlaw:
            if (attitude2 == outlaw)
                return false;
            return true;
        case confederate:
            if (attitude2 == outlaw)
                return true;
            if (attitude2 == rebel)
                return true;
            return false;
        case rebel:
            if (attitude2 == outlaw)
                return true;
            if (attitude2 == confederate)
                return true;
            return false;
        case neutral:
            if (attitude2 == outlaw)
                return true;
            return false;
        default:
            return false;
    }	
}