package de.msz.games.games.herorealms;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HeroRealmsDecisionOption {
	
	private String id;
	private String text;
	private HeroRealmsAbilityType abilityType;
	private int value;
	
	public static HeroRealmsDecisionOption from(Map<String, Object> map) {
		
		return HeroRealmsDecisionOption.builder()
				.id((String) map.get("id"))
				.text((String) map.get("text"))
				.abilityType(HeroRealmsAbilityType.valueOf((String) map.get("abilityType")))
				.value(((Long) map.get("value")).intValue())
				.build();
	}
}
