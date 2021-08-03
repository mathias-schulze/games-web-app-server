package de.msz.games.games;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import de.msz.games.games.Game.GameParameter;
import de.msz.games.games.GameService.Table;
import lombok.Data;

@RestController
@RequestMapping("/games")
public class GamesController {
	
	@Autowired
	private GameService gameService;
	
	@GetMapping("/list")
	public List<GameParameter> getGameList() {
		return Arrays.stream(Game.values())
				.map(game -> game.getParameter())
				.collect(Collectors.toList());
	}
	
	@GetMapping("/parameter")
	public GameParameter getGameParameter(@RequestParam String id) {
		return Game.valueOf(id).getParameter();
	}
	
	@PostMapping
	@ResponseBody
	public CreateNewGameResponse createNewGame(@RequestBody CreateNewGameRequest createNewGameRequest)
			throws InterruptedException, ExecutionException {
		
		CreateNewGameResponse response = new CreateNewGameResponse();
		
		response.setGameId(gameService.createNewGame(createNewGameRequest.getGame()));
		
		return response;
	}
	
	@Data
	public static class CreateNewGameRequest {
		
		private Game game;
	}
	
	@Data
	public static class CreateNewGameResponse {
		
		private String gameId;
	}
	
	@GetMapping("/tables")
	public List<Table> getActiveGames() throws InterruptedException, ExecutionException {
		return gameService.getTables();
	}
	
	@PostMapping("/{id}/join")
	public void joinGame(@PathVariable("id") String id) throws InterruptedException, ExecutionException {
		gameService.joinGame(id);
	}
	
	@PostMapping("/{id}/start")
	public void startGame(@PathVariable("id") String id) throws InterruptedException, ExecutionException {
		gameService.startGame(id);
	}
	
	@DeleteMapping("/{id}")
	public void deleteGame(@PathVariable("id") String id) throws InterruptedException, ExecutionException {
		gameService.deleteGame(id);
	}
}
