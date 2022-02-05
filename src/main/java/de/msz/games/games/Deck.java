package de.msz.games.games;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
			this.cards = new LinkedList<>(shuffle(cards, 10));
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
	
	private List<T> shuffle(List<T> deck, int cycles) {
		
		List<T> shuffledDeck = new ArrayList<>(deck);
		
		for (int i=0; i<cycles; i++) {
			shuffledDeck = riffleShuffle(shuffledDeck);
			Collections.shuffle(shuffledDeck);
		}
		
		return shuffledDeck;
	}
	
	private List<T> riffleShuffle(List<T> deck) {
		
		List<T> shuffledDeck = new ArrayList<>(deck.size());
		
		Iterator<T> half1 = deck.subList(0, deck.size()/2).iterator();
		Iterator<T> half2 = deck.subList(deck.size()/2, deck.size()).iterator();
		while (half1.hasNext() || half2.hasNext()) {
			if (half1.hasNext()) {
				shuffledDeck.add(half1.next());
			}
			if (half2.hasNext()) {
				shuffledDeck.add(half2.next());
			}
		} 
		
		return shuffledDeck;
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
