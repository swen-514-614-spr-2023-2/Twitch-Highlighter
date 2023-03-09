package com.twitch.bot.twitch_connection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.twitch.bot.model.Channel;
import com.twitch.bot.model.User;

public class ChannelsData {
    private static final Logger LOG = Logger.getLogger(ChannelsData.class.getName());
    static HashMap<String, Channel> channels = new HashMap<>();
    static HashMap<String, User> users = new HashMap<String, User>();

    public static List<Channel> getChannels() {
        return new ArrayList<Channel>(channels.values());
    }

    public static Channel getChannel(String channel, Connection twitch_bot) {
        if (!channel.startsWith("#")){
            channel = "#" + channel;
        }
        if (!channels.containsKey(channel)) {
            channels.put(channel, new Channel(channel, twitch_bot));
        }
        return channels.get(channel);
    }

    public static Channel joinChannel(String channelName, Connection twitch_bot) {
        if (!channelName.startsWith("#")){
            channelName = "#" + channelName;
        }
        Channel channel = new Channel(channelName, twitch_bot);
        twitch_bot.sendCommandMessage("JOIN " + channel.toString().toLowerCase() + "\r\n");
        channels.put(channelName, channel);
        LOG.log(Level.INFO, "> JOIN " + channel);
        return channel;
    }

    public static void stopListeningToChannel(String channelName, Connection twitch_bot) {
        if (!channelName.startsWith("#")){
            channelName = "#" + channelName;
        }
        Channel channel = channels.get(channelName);
        twitch_bot.sendCommandMessage("PART " + channel);
        channels.remove(channel);
        LOG.log(Level.INFO, "> PART " + channel);
    }

    public static User getUser(String ign) {
        if (!users.containsKey(ign)) {
            users.put(ign, new User(ign));
        }
        return users.get(ign);
    }
}
