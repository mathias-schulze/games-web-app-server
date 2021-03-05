package de.msz.games.base;

import java.util.List;

import de.msz.games.base.NotificationService.Notification;
import lombok.Data;

@Data
public class NotificationResponse {
	
	private List<Notification> notifications;
}
