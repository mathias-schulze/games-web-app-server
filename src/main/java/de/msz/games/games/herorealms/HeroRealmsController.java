package de.msz.games.games.herorealms;

import java.util.concurrent.ExecutionException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import de.msz.games.base.NotificationResponse;
import lombok.Data;

@RestController
@RequestMapping("/hero_realms")
public class HeroRealmsController {
	
	@Autowired
	private HeroRealmsTableService tableService;
	
	@Autowired
	private HeroRealmsActionsService actionsService;
	
	@PostMapping("/{gameId}/play_card")
	@ResponseBody
	public NotificationResponse playCard(@PathVariable("gameId") String gameId,
			@RequestBody PlayCardRequest playCardRequest) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.playCard(table, playCardRequest.getCardId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@Data
	public static class PlayCardRequest {
		private String cardId;
	}
	
	@PostMapping("/{gameId}/buy_market_card")
	public NotificationResponse buyMarketCard(@PathVariable("gameId") String gameId,
			@RequestBody BuyMarketCardRequest buyMarketCardRequest)
			throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.buyMarketCard(table, buyMarketCardRequest.getCardId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@Data
	public static class BuyMarketCardRequest {
		private String cardId;
	}
	
	@PostMapping("/{gameId}/buy_fire_gem")
	public NotificationResponse buyFireGem(@PathVariable("gameId") String gameId)
			throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.buyFireGem(table);
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@PostMapping("/{gameId}/end_turn")
	public NotificationResponse endTurn(@PathVariable("gameId") String gameId) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.endTurn(table);
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
}
