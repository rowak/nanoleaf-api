package io.github.rowak.nanoleafapi.util;

public class IPMissingException extends RuntimeException {
	
	private static final String DEFAULT_MESSAGE = "No IPv4 addresses were found.";
	
	public IPMissingException()
	{
		super(DEFAULT_MESSAGE);
	}
	
	public IPMissingException(String message)
	{
		super(message);
	}
}
