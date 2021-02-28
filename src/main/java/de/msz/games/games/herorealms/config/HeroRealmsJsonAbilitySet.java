package de.msz.games.games.herorealms.config;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;

import de.msz.games.games.herorealms.HeroRealmsAbilityLinkage;
import lombok.Data;

@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
@Data
public class HeroRealmsJsonAbilitySet {
	
	private String linkage = HeroRealmsAbilityLinkage.AND.name();
	private List<HeroRealmsJsonAbility> abilities;
}
