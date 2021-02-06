package de.msz.games.base;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import de.msz.games.base.Game.GameParameter;
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
}
