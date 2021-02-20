package de.msz.games.games.herorealms;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import de.msz.games.games.Deck;
import lombok.Setter;

@Service
public class HeroRealmsService {
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	private JsonCard fireGem;
	private JsonCard[] marketDeck;
	private JsonCard[] startingDeck;
	
	@PostConstruct
	private void init() {
		fireGem = readCards("classpath:games/hero_realms/cards/fire_gems_deck.json")[0];
		marketDeck = readCards("classpath:games/hero_realms/cards/market_deck.json");
		startingDeck = readCards("classpath:games/hero_realms/cards/starting_deck.json");
	}
	
	public FireGemsDeck createFireGemsDeck() {
		return (new FireGemsDeck(fireGem.quantity, createCard(fireGem)));
	}
	
	public Deck<HeroRealmsCard> createMarketDeck() {
		return createDeck(marketDeck);
	}
	
	public Deck<HeroRealmsCard> createStartingDeck() {
		return createDeck(startingDeck);
	}
	
	private static Deck<HeroRealmsCard> createDeck(JsonCard[] jsonCards) {
		
		List<HeroRealmsCard> cards = new ArrayList<>();
		for (JsonCard jsonCard : jsonCards) {
			for (int i=0; i<jsonCard.quantity; i++) {
				cards.add(createCard(jsonCard));
			}
		}
		
		Deck<HeroRealmsCard> deck = new Deck<>();
		deck.setCards(cards, true);
		return deck;
	}
	
	private static HeroRealmsCard createCard(JsonCard jsonCard) {
		
		return HeroRealmsCard.builder()
			.name(jsonCard.name)
			.cost(jsonCard.cost)
			.defense(jsonCard.defense)
			.faction(jsonCard.faction == null ? null : HeroRealmsFaction.valueOf(jsonCard.faction))
			.type(HeroRealmsCardType.valueOf(jsonCard.type))
			.image(jsonCard.image)
			.build();
	}
	
	private JsonCard[] readCards(String filename) {
		
		ObjectMapper mapper = new ObjectMapper();
		
		try (InputStream jsonFile = resourceLoader.getResource(filename).getInputStream()) {
			
			return mapper.readValue(jsonFile, JsonCard[].class);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	@JsonFormat(with = JsonFormat.Feature.ACCEPT_CASE_INSENSITIVE_PROPERTIES)
	@Setter
	private static class JsonCard {
		
        String name;
        int cost = 0;
        int defense = 0;
        String faction;
        String type;
        int quantity;
        String image;
	}
}
