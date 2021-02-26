package de.msz.games.games.player;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Player {
	
	public String id;
	public String name;
	
	public static Player from(Map<String, Object> map) {
		
		return Player.builder()
				.id((String) map.get("id"))
				.name((String) map.get("name"))
				.build();
	}
}