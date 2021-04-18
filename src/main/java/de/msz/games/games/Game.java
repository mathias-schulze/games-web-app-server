package de.msz.games.games;

import lombok.Builder;
import lombok.Value;

public enum Game {

	HERO_REALMS("HERO_REALMS", "Hero Realms", "extern/hero_realms/hero_realms_back.jpg", 2, 4),
	HERO_REALMS_CHARACTER_PACKS("HERO_REALMS_CHARACTER_PACKS", "Hero Realms Character Packs", 
			"extern/hero_realms/HR-Character-Packs.png", 2, 4),
	;
	
	private final GameParameter parameter;
	
	Game(String id, String name, String image, int minPlayer, int maxPlayer) {
		parameter = GameParameter.builder()
				.id(id)
				.name(name)
				.image(image)
				.minPlayer(minPlayer).maxPlayer(maxPlayer)
				.build();
	}
	
	public GameParameter getParameter() {
		return parameter;
	}
	
	@Value
	@Builder
	public static class GameParameter {
		
		String id;
		String name;
		String image;
		int minPlayer;
		int maxPlayer;
	}
}
