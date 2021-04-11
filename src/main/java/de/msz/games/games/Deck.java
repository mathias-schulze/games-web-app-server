package de.msz.games.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import lombok.Data;

@Data
public class Deck<T extends Card> {
	
	protected int size;
	
	private LinkedList<T> cards = new LinkedList<>();
	
	public T draw() {
		
		if (!cards.isEmpty()) {
			size--;
			return cards.removeFirst();
		}
		
		return null;
	}
	
	public List<T> draw(int number) {
		
		List<T> drawedCards = new ArrayList<>(number);
		for (int i=0; i<number; i++) {
			Optional.ofNullable(draw()).ifPresent(card -> drawedCards.add(card));
		}
		
		return drawedCards;
	}
	
	public final void setCards(List<T> cards) {
		setCards(cards, false);
	}
	
	public final void setCards(List<T> cards, boolean shuffle) {
		
		this.cards = new LinkedList<>(cards);
		size = cards.size();
		
		if (shuffle) {
			for (int i=0; i<10; i++) {
				Collections.shuffle(this.cards);
			}
		}
	}
	
	public List<T> getCards() {
		return Collections.unmodifiableList(cards);
	}
	
	public void addCard(T card) {
		cards.add(card);
		size = cards.size();
	}
	
	public void addCards(List<T> newCards) {
		cards.addAll(newCards);
		size = cards.size();
	}
	
	public void addCardTop(T card) {
		cards.add(0, card);
		size = cards.size();
	}
	
	public void removeCard(T card) {
		cards.remove(card);
		size = cards.size();
	}
	
	public void clear() {
		cards.clear();
		size = 0;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Card> Deck<T> from(Map<String, Object> map, Class<T> clazz) {
		
		List<T> cards = ((List<Map<String, Object>>) map.get("cards")).stream()
				.map(card -> Card.from(card, clazz))
				.collect(Collectors.toList());
		
		Deck<T> deck = new Deck<>();
		deck.setCards(cards);
		deck.setSize(((Long) map.get("size")).intValue());
		
		return deck;
	}
}
