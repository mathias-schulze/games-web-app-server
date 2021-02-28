package de.msz.games.games.herorealms.config;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
@Data
public class HeroRealmsJsonCard {
	
    private String name;
    private int cost = 0;
    private int defense = 0;
    private String faction;
    private String type;
    private int quantity;
    private String image;
    private HeroRealmsJsonAbilitySet PrimaryAbility;
    private HeroRealmsJsonAbilitySet AllyAbility;
    private HeroRealmsJsonAbilitySet SacrificeAbility;
}