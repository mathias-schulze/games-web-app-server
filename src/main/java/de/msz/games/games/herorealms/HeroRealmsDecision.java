package de.msz.games.games.herorealms;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HeroRealmsDecision {
	
	private String id;
	private String cardId;
	private HeroRealmsDecisionType type;
	private String text;
	private List<HeroRealmsDecisionOption> options;
	
	@SuppressWarnings("unchecked")
	public static HeroRealmsDecision from(Map<String, Object> map) {
		
		List<HeroRealmsDecisionOption> options = ((List<Map<String, Object>>) map.get("options")).stream()
				.map(option -> HeroRealmsDecisionOption.from(option))
				.collect(Collectors.toList());
		
		return HeroRealmsDecision.builder()
				.id((String) map.get("id"))
				.cardId((String) map.get("cardId"))
				.type(HeroRealmsDecisionType.valueOf((String) map.get("type")))
				.text((String) map.get("text"))
				.options(options)
				.build();
	}
}
