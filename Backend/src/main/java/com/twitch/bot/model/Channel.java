package com.twitch.bot.model;

import com.twitch.bot.twitch_connection.Connection;

public class Channel {
    private String channel;
	private Connection twitch_bot;

    public Channel(String channel, Connection twitch_bot){
        this.channel = channel;
        this.twitch_bot = twitch_bot;
    }
    
    public String getChannel() {
        return channel;
    }

    public Connection getTwitch_bot() {
        return twitch_bot;
    }

    
    @Override
    public final String toString() {
		return channel;
	}
}
