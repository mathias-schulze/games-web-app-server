package de.msz.games.base;

import lombok.Builder;
import lombok.Value;

public enum Game {

	HERO_REALMS("HERO_REALMS", "Hero Realms", "extern/hero_realms/hero_realms_back.jpg", 2, 4),
	;
	
	private final GameParameter parameter;
	
	Game(String id, String name, String image, int minPlayer, int maxPlayer) {
		parameter = GameParameter.builder()
				.id(id)
				.name(name)
				.image(image)
				.minPlayer(2).maxPlayer(4)
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
