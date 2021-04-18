package de.msz.games.games.herorealms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum HeroRealmsCharacterPack {
	
	CLERIC,
	FIGHTER,
	RANGER,
	THIEF,
	WIZARD,
	;
	
	public static List<HeroRealmsCharacterPack> getRandomList() {
		
		List<HeroRealmsCharacterPack> packs = new ArrayList<>(Arrays.asList(values()));
		Collections.shuffle(packs);
		return packs;
	}
}
