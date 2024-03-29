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
	
	@PostMapping("/{gameId}/play_champion")
	@ResponseBody
	public NotificationResponse playChampion(@PathVariable("gameId") String gameId,
			@RequestBody PlayChampionRequest playChampionRequest) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.playChampion(table, playChampionRequest.getChampionId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@Data
	public static class PlayChampionRequest {
		private String championId;
	}
	
	@PostMapping("/{gameId}/make_decision")
	@ResponseBody
	public NotificationResponse makeDecision(@PathVariable("gameId") String gameId,
			@RequestBody MakeDecisionRequest makeDecisionRequest) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.makeDecision(table, makeDecisionRequest.getDecisionId(), makeDecisionRequest.getOptionId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@Data
	public static class MakeDecisionRequest {
		private String decisionId;
		private String optionId;
	}
	
	@PostMapping("/{gameId}/sacrifice")
	@ResponseBody
	public NotificationResponse sacrifice(@PathVariable("gameId") String gameId,
			@RequestBody SacrificeCardRequest sacrificeCardRequest) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.sacrifice(table, sacrificeCardRequest.getCardId(), sacrificeCardRequest.isWithAbility());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@Data
	public static class SacrificeCardRequest {
		private String cardId;
		private boolean withAbility;
	}
	
	@PostMapping("/{gameId}/discard")
	@ResponseBody
	public NotificationResponse discard(@PathVariable("gameId") String gameId,
			@RequestBody PlayCardRequest playCardRequest) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.discard(table, playCardRequest.getCardId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@PostMapping("/{gameId}/select_player_for_discard")
	@ResponseBody
	public NotificationResponse selectPlayer4Discard(@PathVariable("gameId") String gameId,
			@RequestBody SelectPlayerRequest selectPlayerRequest) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.selectPlayer4Discard(table, selectPlayerRequest.getPlayerId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@Data
	public static class SelectPlayerRequest {
		private String playerId;
	}
	
	@PostMapping("/{gameId}/prepare_champion")
	@ResponseBody
	public NotificationResponse prepareChampion(@PathVariable("gameId") String gameId,
			@RequestBody PlayCardRequest playCardRequest) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.prepareChampion(table, playCardRequest.getCardId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@PostMapping("/{gameId}/stun_target_champion")
	@ResponseBody
	public NotificationResponse stunTargetChampion(@PathVariable("gameId") String gameId,
			@RequestBody StunTargetChampionRequest stunTargetChampionRequest)
			throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.stunTargetChampion(table, 
				stunTargetChampionRequest.getPlayerId(), stunTargetChampionRequest.getChampionId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@Data
	public static class StunTargetChampionRequest {
		private String playerId;
		private String championId;
	}
	
	@PostMapping("/{gameId}/put_card_top_deck")
	@ResponseBody
	public NotificationResponse putCardTopDeck(@PathVariable("gameId") String gameId,
			@RequestBody PlayCardRequest playCardRequest) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.putCardTopDeck(table, playCardRequest.getCardId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@PostMapping("/{gameId}/attack")
	@ResponseBody
	public NotificationResponse attack(@PathVariable("gameId") String gameId,
			@RequestBody AttackRequest attackRequest) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.attack(table, attackRequest.getPlayerId(), attackRequest.getChampionId(),
				attackRequest.getValue(), false);
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@Data
	public static class AttackRequest {
		private String playerId;
		private String championId;
		private int value;
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
	
	@PostMapping("/{gameId}/process_character_round_abilities")
	public NotificationResponse executeCharacterRoundAbilities(@PathVariable("gameId") String gameId)
			throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.processCharacterRoundAbilities(table);
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}

	@PostMapping("/{gameId}/process_character_one_time_abilities")
	public NotificationResponse executeCharacterOneTimeAbilities(@PathVariable("gameId") String gameId)
			throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.processCharacterOneTimeAbilities(table);
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@PostMapping("/{gameId}/select_player_for_bless")
	@ResponseBody
	public NotificationResponse selectPlayer4Bless(@PathVariable("gameId") String gameId,
			@RequestBody SelectPlayerRequest selectPlayerRequest) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.selectPlayer4Bless(table, selectPlayerRequest.getPlayerId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@PostMapping("/{gameId}/put_champion_discard_into_play")
	@ResponseBody
	public NotificationResponse putChampionDiscardIntoPlay(@PathVariable("gameId") String gameId,
			@RequestBody PlayCardRequest selectCardRequest) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.putChampionDiscardIntoPlay(table, selectCardRequest.getCardId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@PostMapping("/{gameId}/ranger_track_discard")
	@ResponseBody
	public NotificationResponse rangerTrackDiscard(@PathVariable("gameId") String gameId,
			@RequestBody PlayCardRequest selectCardRequest) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.rangerTrackDiscard(table, selectCardRequest.getCardId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@PostMapping("/{gameId}/ranger_track_up")
	@ResponseBody
	public NotificationResponse rangerTrackUp(@PathVariable("gameId") String gameId,
			@RequestBody PlayCardRequest selectCardRequest) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.rangerTrackUp(table, selectCardRequest.getCardId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@PostMapping("/{gameId}/ranger_track_down")
	@ResponseBody
	public NotificationResponse rangerTrackDown(@PathVariable("gameId") String gameId,
			@RequestBody PlayCardRequest selectCardRequest) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.rangerTrackDown(table, selectCardRequest.getCardId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@PostMapping("/{gameId}/ranger_track_end")
	@ResponseBody
	public NotificationResponse rangerTrackEnd(@PathVariable("gameId") String gameId)
			throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.rangerTrackEnd(table);
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@PostMapping("/{gameId}/acquire_opponent_discard")
	@ResponseBody
	public NotificationResponse acquireOpponentDiscard(@PathVariable("gameId") String gameId,
			@RequestBody AcquireOpponentDiscardRequest acquireOpponentDiscardRequest)
			throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.acquireOpponentDiscard(table, acquireOpponentDiscardRequest.getPlayerId(), 
				acquireOpponentDiscardRequest.getCardId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@PostMapping("/{gameId}/select_player_for_fireball")
	@ResponseBody
	public NotificationResponse selectPlayer4Fireball(@PathVariable("gameId") String gameId,
			@RequestBody SelectPlayerRequest selectPlayerRequest) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.selectPlayer4Fireball(table, selectPlayerRequest.getPlayerId());
		tableService.storeTable(gameId, table);
		
		return (new NotificationResponse());
	}
	
	@Data
	public static class AcquireOpponentDiscardRequest {
		private String playerId;
		private String cardId;
	}

	@PostMapping("/{gameId}/cancel_special_action_mode")
	public NotificationResponse endSpecialActionMode(@PathVariable("gameId") String gameId) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		actionsService.cancelSpecialActionMode(table);
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
	
	@PostMapping("/{gameId}/start_observer_mode")
	public NotificationResponse startObserverMode(@PathVariable("gameId") String gameId) throws InterruptedException, ExecutionException {
		
		HeroRealmsTable table = (HeroRealmsTable) tableService.getGameTable(gameId);
		if (!table.isObserverMode()) {
			table.setObserverMode(true);
			tableService.storeTable(gameId, table);
		}
		
		return (new NotificationResponse());
	}
}
