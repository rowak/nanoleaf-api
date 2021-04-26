package io.github.rowak.nanoleafapi.event;

import org.json.JSONArray;
import org.json.JSONObject;

import com.here.oksse.ServerSentEvent;

import okhttp3.Request;
import okhttp3.Response;

public interface NanoleafEventListener extends ServerSentEvent.Listener {
	
	/**
	 * Called when the connection has been successfully opened.
	 */
	public void onOpen();
	
	/**
	 * Called when the connection has been successfully closed.
	 */
	public void onClosed();
	
	/**
	 * Called when one or more events have been received.
	 * @param events  an array of one or more events
	 */
	public void onEvent(Event[] events);
	
	@Override
	public default void onClosed(ServerSentEvent sse) {
		onClosed();
	}

	@Override
	public default void onComment(ServerSentEvent sse, String comment) {}

	@Override
	public default void onMessage(ServerSentEvent sse, String id, String event, String message) {
		System.out.println(id + " " + event + " " + message);
		int idNum = Integer.parseInt(id);
		EventType eventType = EventType.values()[idNum-1];
		JSONObject messageJson = new JSONObject(message);
		JSONArray eventsJson = messageJson.getJSONArray("events");
		Event[] events = new Event[eventsJson.length()];
		for (int i = 0; i < eventsJson.length(); i++) {
			JSONObject eventJson = eventsJson.getJSONObject(i);
			events[i] = Event.fromJSON(eventType, eventJson);
		}
		onEvent(events);
	}

	@Override
	public default void onOpen(ServerSentEvent sse, Response response) {
		onOpen();
	}

	@Override
	public default Request onPreRetry(ServerSentEvent sse, Request request) {
		return null;
	}

	@Override
	public default boolean onRetryError(ServerSentEvent sse, Throwable error, Response response) {
		return true;
	}

	@Override
	public default boolean onRetryTime(ServerSentEvent sse, long millis) {
		return true;
	}
}
