package com.twitch.bot.controller;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.twitch.bot.twitch_connection.ChannelsData;
import com.twitch.bot.twitch_connection.Connection;

@CrossOrigin(origins = "http://localhost:5173/")
@RestController
public class Controller {
    private static final Logger LOG = Logger.getLogger(Controller.class.getName());
    private Connection twitch_connection;
    HttpHeaders responseHeaders = new HttpHeaders();
    
    public Controller(ChannelsData channelsData, Connection twitch_connection) throws Exception{
        this.twitch_connection = twitch_connection;
        responseHeaders.set("Access-Control-Allow-Headers", "Content-Type,X-Amz-Date,Authorization,X-Api-Key,X-Amz-Security-Token");
        responseHeaders.set("Access-Control-Allow-Methods", "OPTIONS,POST,GET,DELETE,PUT");
        responseHeaders.set("Access-Control-Allow-Credentials", "true");
        responseHeaders.set("Access-Control-Allow-Origin", "*");
        responseHeaders.set("X-Requested-With", "*");
    }

    @GetMapping("/channels")
    public ResponseEntity<Object> getTwitchChannels() throws Exception {
        LOG.log(Level.INFO, "Inside Channels");
        return new ResponseEntity<>(twitch_connection.getAllChannels(), responseHeaders, HttpStatus.OK);
    }

    @GetMapping("/twitch_analysis")
    public ResponseEntity<Object> getTwitchAnalysisData(@RequestParam("channel_name") String channelName) throws Exception {
        LOG.log(Level.INFO, "Inside twitch_analysis");
        HashMap<String, Object> response = new HashMap<>();
        response.put("twitch_analysis", twitch_connection.getTwitchAnalysisOfAChannel(channelName).toList().stream().map(m -> ((HashMap<String, Object>) m)).collect(Collectors.toList()));
        response.put("channel_name", channelName);
        return new ResponseEntity<>(response, responseHeaders, HttpStatus.OK);
    }

    @GetMapping("/channel_broadcastId")
    public ResponseEntity<Object> getChannelBroadcastId(@RequestParam("channel_name") String channelName) throws Exception {
        LOG.log(Level.INFO, "Inside channel_broadcastId");
        return new ResponseEntity<>(twitch_connection.getUserBroadcasterId(channelName), responseHeaders, HttpStatus.OK);
    }

    @PostMapping("/addChannel")
    public ResponseEntity<Object> subscribeChannel(@RequestParam("channel_name") String channelName) throws Exception {
        twitch_connection.addAndJoinChannel(channelName);
        return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
    }

    @DeleteMapping("/removeChannel")
    public ResponseEntity<Object> unSubscribeChannel(@RequestParam("channel_name") String channelName) throws Exception {
        twitch_connection.removeAndDeleteChannelData(channelName);
        return new ResponseEntity<>(responseHeaders, HttpStatus.OK);
    }


    // @GetMapping("/addChannel")
    // public ResponseEntity<Object> subscribeChannel() throws Exception {
    //     twitch_connection.connect();
    //     twitch_connection.joinChannel("tubbo");
    //     return new ResponseEntity<>(HttpStatus.OK);
    // }

    // @GetMapping("/removeChannel")
    // public ResponseEntity<Object> unSubscribeChannel() throws Exception {
    //     twitch_connection.removeChannel("tubbo");
    //     return new ResponseEntity<>(HttpStatus.OK);
    // }

    // @GetMapping("/clips")
    // public ResponseEntity<Object> getClips() throws Exception {
    //     twitch_connection.getUsers();
    //     return new ResponseEntity<>(HttpStatus.OK);
    // }
}
