package de.msz.games.games;

import java.util.Map;

import de.msz.games.games.herorealms.HeroRealmsCard;

public interface Card {
	
	@SuppressWarnings("unchecked")
	public static <T extends Card> T from(Map<String, Object> map, Class<T> clazz) {
		
		if (clazz == HeroRealmsCard.class) {
			return (T) HeroRealmsCard.from(map);
		}
		
		throw new IllegalArgumentException("unknown class " + clazz.getName());
	}
}
