var factionRank = function (attitude, confederateRep, rebelRep, confederate, rebel, rank1, rank2, rank3) {
    if (attitude == confederate)
    {
        if (confederateRep > rank3)
            return 3;
        if (confederateRep > rank2)
            return 2;
        if (confederateRep > rank1)
            return 1;
    }
    if (attitude == rebel)
    {
        if (rebelRep > rank3)
            return 3;
        if (rebelRep > rank2)
            return 2;
        if (rebelRep > rank1)
            return 1;
    }
    return 0;
}