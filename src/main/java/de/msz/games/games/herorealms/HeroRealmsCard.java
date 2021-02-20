package de.msz.games.games.herorealms;

import de.msz.games.games.Card;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class HeroRealmsCard implements Card {

    private String name;
    private int cost;
    private int defense;
    private HeroRealmsFaction faction;
    private HeroRealmsCardType type;
    private String image;
}
