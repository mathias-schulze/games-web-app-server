package de.msz.games.base;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class NotificationAdvice implements ResponseBodyAdvice<NotificationResponse> {
	
	@Autowired
	private NotificationService notificationService;
	
	@Override
	public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
		return (returnType.getGenericParameterType().getTypeName().equals(NotificationResponse.class.getName()));
	}
	
	@Override
	public NotificationResponse beforeBodyWrite(NotificationResponse body, MethodParameter returnType,
			MediaType selectedContentType, Class<? extends HttpMessageConverter<?>> selectedConverterType,
			ServerHttpRequest request, ServerHttpResponse response) {
		
		body.setNotifications(notificationService.getNotifications());
		
		return body;
	}
}
