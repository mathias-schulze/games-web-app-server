package de.msz.games.games.herorealms;

import java.util.Map;
import java.util.Optional;

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
    @Builder.Default
    private int damage = 0;
    private HeroRealmsFaction faction;
    private HeroRealmsCardType type;
    private HeroRealmsCardSubType subType;
    private String image;
    @Builder.Default
    private boolean ready = true;
    @Builder.Default
    private boolean sacrifice = false;
    @Builder.Default
    private boolean stunnedSinceLastTurn = false;
    
    public static HeroRealmsCard from(Map<String, Object> map) {
    	
    	String factionString = (String) map.get("faction");
    	HeroRealmsFaction faction = (factionString == null) ? null : HeroRealmsFaction.valueOf(factionString);
    	
    	String subTypeString = (String) map.get("subType");
    	HeroRealmsCardSubType subType = (subTypeString == null) ? null : HeroRealmsCardSubType.valueOf(subTypeString);
    	
		return HeroRealmsCard.builder()
			.id((String) map.get("id"))
    		.name((String) map.get("name"))
    		.cost(((Long) map.get("cost")).intValue())
    		.defense(((Long) map.get("defense")).intValue())
    		.damage(Optional.ofNullable((Long) map.get("damage")).orElse(0L).intValue())
    		.faction(faction)
    		.type(HeroRealmsCardType.valueOf((String) map.get("type")))
    		.subType(subType)
    		.image((String) map.get("image"))
    		.ready((Boolean) map.get("ready"))
    		.sacrifice((Boolean) map.get("sacrifice"))
    		.stunnedSinceLastTurn(Optional.ofNullable((Boolean) map.get("stunnedSinceLastTurn")).orElse(false))
    		.build();
    }
}
