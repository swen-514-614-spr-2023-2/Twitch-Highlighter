package com.twitch.bot.twitch_connection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import com.twitch.bot.api.ApiHandler;
import com.twitch.bot.api.ApiHandler.PATH;
import com.twitch.bot.db_utils.TwitchData;
import com.twitch.bot.model.Channel;

@Component
public class Connection {
    private static final Logger LOG = Logger.getLogger(Connection.class.getName());
    private ApiHandler apiHandler;
    private Boolean isConnectionRunning = false;
    private Boolean isStartReadingMessagesStarted = false;
    private BufferedReader twitch_reader;
    private BufferedWriter twitch_writer;
    private TwitchData twitchData;

    public BufferedReader getTwitch_reader() {
        return twitch_reader;
    }

    public BufferedWriter getTwitch_writer() {
        return twitch_writer;
    }

    public Connection(ApiHandler apiHandler, TwitchData twitchData) throws Exception {
        this.apiHandler = apiHandler;
        this.twitchData = twitchData;
        this.connect();
        HashMap<String, Channel> channels = ChannelsData.getChannels();
        Iterator<String> channelsIter = channels.keySet().iterator();
        while(channelsIter.hasNext()){
            Channel channel = channels.get(channelsIter.next());
            this.joinChannel(channel.getChannelName());
        }
    }

    public void sendCommandMessage(Object message) {
        try {
            this.twitch_writer.write(message + " \r\n");
            this.twitch_writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.log(Level.INFO, message.toString());
    }

    public void sendMessage(Object message, Channel channel) {
        try {
            this.twitch_writer.write("PRIVMSG " + "#" + channel.getChannelName() + " :" + message.toString() + "\r\n");
            this.twitch_writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.log(Level.INFO, message.toString());
    }

    public void addAndJoinChannel(String channelName) throws Exception{
        String broadcaster_id = getUserBroadcasterId(channelName);
        new ChannelsData(twitchData).addChannel(channelName, broadcaster_id);
        joinChannel(channelName);
    }

    
    public void removeAndDeleteChannelData(String channelName) throws Exception{
        Channel channel = ChannelsData.getChannel(channelName);
        if(channel != null){
            removeChannel(channel.getChannelName());
            new ChannelsData(twitchData).removeChannel(channel);
        }
    }

    public void joinChannel(String channelName){
        ChannelsData.joinChannel(channelName, this);
    }

    public void removeChannel(String channelName){
        ChannelsData.stopListeningToChannel(channelName, this);
    }

     /*
     * If the server stops unexpectly, on restart all connected channels must be cleared
     */
    public void cleanUp(){
        ChannelsData.stopListeningToChannel("tubbo", this);
    }

    public Boolean connect() throws Exception {
        Boolean isFirstTimeConnect = !isConnectionRunning;
        if (!isConnectionRunning) {
            isConnectionRunning = apiHandler.CONNECT();
        }
        this.twitch_writer = apiHandler.getTwitch_writer();
        this.twitch_reader = apiHandler.getTwitch_reader();
        if(isFirstTimeConnect){
            cleanUp();
        }
        startReadingMessages();
        return isConnectionRunning;
    }

    private void startReadingMessages() {
        if (isStartReadingMessagesStarted) {
            return;
        }
        isStartReadingMessagesStarted = true;
        new Thread(() -> {
            readTwitchMessage();
        }).start();
    }

    public void readTwitchMessage(){
        String currentLine = "";
        try {
            while ((currentLine = this.twitch_reader.readLine()) != null) {
                if (currentLine.toLowerCase().startsWith("ping")) {
                    processPingMessage(currentLine);
                } else if (currentLine.contains("PRIVMSG")) {
                    processMessage(currentLine);
                } else if (currentLine.toLowerCase().contains("disconnected")) {
                    LOG.log(Level.INFO, currentLine);
                    apiHandler.CONNECT();
                } else {
                    LOG.log(Level.INFO, currentLine);
                }
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception is " + ex);
            isStartReadingMessagesStarted= false;
        }
    }

    private void processPingMessage(String currentLine) throws Exception {
        this.twitch_writer.write("PONG " + currentLine.substring(5) + "\r\n");
        this.twitch_writer.flush();
    }

    private void processMessage(String currentLine) {
        String str[];
        str = currentLine.split("!");
        String msg_user = str[0].substring(1, str[0].length());
        str = currentLine.split(" ");
        Channel msg_channel = ChannelsData.getChannel(str[2].startsWith("#") ? str[2].substring(1) : str[2]);
        String msg_msg = currentLine.substring((str[0].length() + str[1].length() + str[2].length() + 4), currentLine.length());
        LOG.log(Level.INFO, "Channel Details : " + msg_channel + " ||| User : " + msg_user + " ||| Messsage : " + msg_msg);
        if (msg_msg.startsWith("!")){
            processCommand(msg_user, msg_channel, msg_msg.substring(1));
        }
        // if (msg_user.toString().equals("jtv") && msg_msg.contains("now hosting")) {
        //     String hoster = msg_msg.split(" ")[0];
        //     processHost(ChannelsData.getUser(hoster), msg_channel);
        // }
        processMessage(msg_user, msg_channel, msg_msg);
    }

    private void processCommand(String user, Channel channel, String command){

    }

    private void processMessage(String user, Channel channel, String message)
	{
        twitchData.addTwitchMessage(user, channel, message, System.currentTimeMillis());
	}

    // private void processHost(User hoster, Channel hosted)
	// {
	
	// }

    public String getUserBroadcasterId(String name) throws Exception{
        String response = apiHandler.setPath(PATH.GET_USERS).setParams(new JSONObject().put("login", name)).setHeaders(new JSONObject().put("set_client_id", "Client-Id")).GET();
        JSONObject responseData = new JSONObject(response);
        String broadcaster_id = responseData.getJSONArray("data").getJSONObject(0).getString("id");
        LOG.log(Level.INFO, "broadcaster_id :::: " + broadcaster_id);
        return broadcaster_id;
    }

    public void makeClips(String broadcaster_id) throws Exception{
        String response = apiHandler.setPath(PATH.CLIPS).setParams(new JSONObject().put("broadcaster_id", broadcaster_id)).setHeaders(new JSONObject().put("set_client_id", "Client-Id")).POST();
        JSONObject responseData = new JSONObject(response);
        String clip_id = responseData.getJSONArray("data").getJSONObject(0).getString("id");
        LOG.log(Level.INFO, "clip_id :::: " + clip_id);
        Thread.sleep(500);//*Thread Sleeps so that the create clip is done generating on twitch side */
        response = apiHandler.setPath(PATH.CLIPS).setParams(new JSONObject().put("id", "GoodObedientGazelleBigBrother-4fwYP4VvZEr4BcNg")).setHeaders(new JSONObject().put("set_client_id", "Client-Id")).GET();
        LOG.log(Level.INFO, "response :::: " + response);
    }

    public JSONArray getTwitchAnalysisOfAChannel(String channelName){
        return twitchData.getTwitchAnalysisOfAChannel(ChannelsData.getChannel(channelName));
    }
}
