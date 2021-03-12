package de.msz.games.games.herorealms;

import java.util.Map;

import de.msz.games.games.Card;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HeroRealmsCard implements Card {
	
	private String id;
    private String name;
    private int cost;
    private int defense;
    private HeroRealmsFaction faction;
    private HeroRealmsCardType type;
    private String image;
    @Builder.Default
    private boolean ready = true;
    
    public static HeroRealmsCard from(Map<String, Object> map) {
    	
    	String factionString = (String) map.get("faction");
    	HeroRealmsFaction faction = (factionString == null) ? null : HeroRealmsFaction.valueOf(factionString);
    	
		return HeroRealmsCard.builder()
			.id((String) map.get("id"))
    		.name((String) map.get("name"))
    		.cost(((Long) map.get("cost")).intValue())
    		.defense(((Long) map.get("defense")).intValue())
    		.faction(faction)
    		.type(HeroRealmsCardType.valueOf((String) map.get("type")))
    		.image((String) map.get("image"))
    		.ready((Boolean) map.get("ready"))
    		.build();
    }
}
