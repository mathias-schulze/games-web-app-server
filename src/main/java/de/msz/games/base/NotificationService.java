package de.msz.games.base;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;

import lombok.Value;

@Service
@RequestScope
public class NotificationService {
	
	private List<Notification> notifications = new ArrayList<>();
	
	public List<Notification> getNotifications() {
		return Collections.unmodifiableList(notifications);
	}
	
	public void addNotification(NotificationType type, String message) {
		notifications.add(new Notification(type, message));
	}
	
	@Value
	public static class Notification {
		
		private final NotificationType type;
		private final String message;
	}
	
	public static enum NotificationType {
		
		ERROR,
		SUCCESS,
		WARNING,
		INFO;
	}
}
