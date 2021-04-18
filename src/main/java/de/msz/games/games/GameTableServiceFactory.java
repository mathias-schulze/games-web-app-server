package de.msz.games.games;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.msz.games.games.herorealms.HeroRealmsTableService;

@Service
public class GameTableServiceFactory {
	
	@Autowired
	private HeroRealmsTableService heroRealmsTableService;
	
	public GameTableService getService(Game game) {
		
		GameTableService gameTableService = null;
		switch (game) {
			case HERO_REALMS:
			case HERO_REALMS_CHARACTER_PACKS:
				gameTableService = heroRealmsTableService;
				break;
			default:
				throw new IllegalArgumentException("unknown game " + game.name());
		}
		
		return gameTableService;
	}
}
