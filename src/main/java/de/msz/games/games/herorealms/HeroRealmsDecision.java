package de.msz.games.games.herorealms;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
	@Builder.Default
	private boolean active = false;
	private List<HeroRealmsDecisionOption> options;
	
	@SuppressWarnings("unchecked")
	public static HeroRealmsDecision from(Map<String, Object> map) {
		
		List<HeroRealmsDecisionOption> options = Optional.ofNullable((List<Map<String, Object>>) map.get("options"))
				.orElse(Collections.emptyList()).stream()
				.map(option -> HeroRealmsDecisionOption.from(option))
				.collect(Collectors.toList());
		
		return HeroRealmsDecision.builder()
				.id((String) map.get("id"))
				.cardId((String) map.get("cardId"))
				.type(HeroRealmsDecisionType.valueOf((String) map.get("type")))
				.text((String) map.get("text"))
				.active(Optional.ofNullable((Boolean) map.get("active")).orElse(false))
				.options(options)
				.build();
	}
}
