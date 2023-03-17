package com.twitch.bot.twitch_connection;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.stereotype.Component;

import com.twitch.bot.db_utils.TwitchData;
import com.twitch.bot.model.Channel;

@Component
public class ChannelsData {
    private static final Logger LOG = Logger.getLogger(ChannelsData.class.getName());
    private static HashMap<String, Channel> channels = new HashMap<>();

    public static HashMap<String, Channel> getChannels() {
        return channels;
    }

    static void setChannelDetails(List<Channel> channelsData){
        Iterator<Channel> channelsDataIter = channelsData.iterator();
        while(channelsDataIter.hasNext()){
            Channel channel = channelsDataIter.next();
            channels.put(channel.getChannelName(), channel);
        }
    }

    public ChannelsData(TwitchData twitchData){
        setChannelDetails(twitchData.getChannelDetails());
    }

    public static Channel getChannel(String channel, Connection twitch_bot) {
        return channels.get(channel);
    }

    public static Channel joinChannel(String channelName, Connection twitch_bot) {
        Channel channel = channels.get(channelName);
        twitch_bot.sendCommandMessage("JOIN " + "#" + channelName + "\r\n");
        LOG.log(Level.INFO, "> JOIN " + channelName);
        channel.setIsListeningToChannel(true);
        channels.put(channelName, channel);
        return channel;
    }

    public static void stopListeningToChannel(String channelName, Connection twitch_bot) {
        Channel channel = channels.get(channelName);
        twitch_bot.sendCommandMessage("PART " + "#" + channelName);
        channel.setIsListeningToChannel(false);
        channels.put(channelName, channel);
        LOG.log(Level.INFO, "> PART " + channelName);
    }
}
