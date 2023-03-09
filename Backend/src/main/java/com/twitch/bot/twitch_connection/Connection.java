package com.twitch.bot.twitch_connection;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.twitch.bot.api.ApiHandler;
import com.twitch.bot.model.Channel;
import com.twitch.bot.model.User;

@Component
public class Connection {
    private static final Logger LOG = Logger.getLogger(Connection.class.getName());
    private ApiHandler apiHandler;
    private Boolean isConnectionRunning = false;
    private Boolean isStartReadingMessagesStarted = false;
    private BufferedReader twitch_reader;
    private BufferedWriter twitch_writer;

    public BufferedReader getTwitch_reader() {
        return twitch_reader;
    }

    public BufferedWriter getTwitch_writer() {
        return twitch_writer;
    }

    public Connection(ApiHandler apiHandler) {
        this.apiHandler = apiHandler;
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
            this.twitch_writer.write("PRIVMSG " + channel.getChannel() + " :" + message.toString() + "\r\n");
            this.twitch_writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOG.log(Level.INFO, message.toString());
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
        Channel msg_channel;
        msg_channel = ChannelsData.getChannel(str[2], this);
        User user = ChannelsData.getUser(msg_user);
        String msg_msg = currentLine.substring((str[0].length() + str[1].length() + str[2].length() + 4), currentLine.length());
        LOG.log(Level.INFO, "> " + msg_channel + " | " + msg_user + " >> " + msg_msg);
        if (msg_msg.startsWith("!")){
            processCommand(user, msg_channel, msg_msg.substring(1));
        }
        if (msg_user.toString().equals("jtv") && msg_msg.contains("now hosting")) {
            String hoster = msg_msg.split(" ")[0];
            processHost(ChannelsData.getUser(hoster), msg_channel);
        }
        processMessage(user, msg_channel, msg_msg);
    }

    private void processCommand(User user, Channel channel, String command){

    }

    private void processMessage(User user, Channel channel, String message)
	{
		
	}

    private void processHost(User hoster, Channel hosted)
	{
	
	}
}
