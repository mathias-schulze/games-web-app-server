package de.msz.games.games.herorealms;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.commons.collections4.map.HashedMap;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import de.msz.games.games.Deck;
import de.msz.games.games.herorealms.config.HeroRealmsJsonAbility;
import de.msz.games.games.herorealms.config.HeroRealmsJsonAbilitySet;
import de.msz.games.games.herorealms.config.HeroRealmsJsonCard;

@Service
public class HeroRealmsService {
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	@Autowired
	private JsonConfigMapper jsonConfigMapper;
	
	private HeroRealmsJsonCard[] fireGemDeck;
	private HeroRealmsJsonCard[] marketDeck;
	private HeroRealmsJsonCard[] startingDeck;
	
	private Map<String, HeroRealmsCardAbilities> cardAbilities = new HashedMap<>();
	
	@PostConstruct
	private void init() {
		fireGemDeck = readCards("classpath:games/hero_realms/cards/fire_gems_deck.json");
		marketDeck = readCards("classpath:games/hero_realms/cards/market_deck.json");
		startingDeck = readCards("classpath:games/hero_realms/cards/starting_deck.json");
		
		for (HeroRealmsJsonCard fireGemDeckCard : fireGemDeck) {
			addCardAbilities(fireGemDeckCard);
		}
		for (HeroRealmsJsonCard marketDeckCard : marketDeck) {
			addCardAbilities(marketDeckCard);
		}
		for (HeroRealmsJsonCard startingDeckCard : startingDeck) {
			addCardAbilities(startingDeckCard);
		}
	}
	
	public Deck<HeroRealmsCard> createFireGemsDeck() {
		return createDeck(fireGemDeck);
	}
	
	public Deck<HeroRealmsCard> createMarketDeck() {
		return createDeck(marketDeck);
	}
	
	public Deck<HeroRealmsCard> createStartingDeck() {
		return createDeck(startingDeck);
	}
	
	private Deck<HeroRealmsCard> createDeck(HeroRealmsJsonCard[] jsonCards) {
		
		List<HeroRealmsCard> cards = new ArrayList<>();
		for (HeroRealmsJsonCard jsonCard : jsonCards) {
			for (int i=0; i<jsonCard.getQuantity(); i++) {
				HeroRealmsCard card = jsonConfigMapper.jsonCardToCard(jsonCard);
				card.setId(UUID.randomUUID().toString());
				card.setSacrifice(jsonCard.getSacrificeAbility() != null);
				cards.add(card);
			}
		}
		
		Deck<HeroRealmsCard> deck = new Deck<>();
		deck.setCards(cards, true);
		return deck;
	}
	
	private void addCardAbilities(HeroRealmsJsonCard jsonCard) {
		
		cardAbilities.put(jsonCard.getName(), HeroRealmsCardAbilities.builder()
				.primaryAbility(jsonConfigMapper.jsonAbilitySetToAbilitySet(jsonCard.getPrimaryAbility()))
				.allyAbility(jsonConfigMapper.jsonAbilitySetToAbilitySet(jsonCard.getAllyAbility()))
				.sacrificeAbility(jsonConfigMapper.jsonAbilitySetToAbilitySet(jsonCard.getSacrificeAbility()))
				.build()
		);
	}
	
	private HeroRealmsJsonCard[] readCards(String filename) {
		
		ObjectMapper mapper = new ObjectMapper();
		
		try (InputStream jsonFile = resourceLoader.getResource(filename).getInputStream()) {
			
			return mapper.readValue(jsonFile, HeroRealmsJsonCard[].class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@Mapper(componentModel = "spring")
	public interface JsonConfigMapper {
		
		@Mapping(target = "id", ignore = true)
		@Mapping(target = "ready", ignore = true)
		@Mapping(target = "sacrifice", ignore = true)
		HeroRealmsCard jsonCardToCard(HeroRealmsJsonCard source);
		@Mapping(target = "allyAbility", ignore = true)
		@Mapping(target = "primaryAbility", ignore = true)
		@Mapping(target = "quantity", ignore = true)
		@Mapping(target = "sacrificeAbility", ignore = true)
		HeroRealmsJsonCard cardToJsonCard(HeroRealmsCard destination);
		
		HeroRealmsAbilitySet jsonAbilitySetToAbilitySet(HeroRealmsJsonAbilitySet source);
		HeroRealmsJsonAbilitySet abilitySetToJsonAbilitySet(HeroRealmsAbilitySet destination);
		
		HeroRealmsAbility jsonAbilityToAbility(HeroRealmsJsonAbility source);
		HeroRealmsJsonAbility abilityToJsonAbility(HeroRealmsAbility destination);
	}
	
	public HeroRealmsCardAbilities getCardAbilities(String cardName) {
		return cardAbilities.get(cardName);
	}
}
