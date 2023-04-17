package com.twitch.bot.controller;

import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.twitch.bot.model.Subscriptions;
import com.twitch.bot.model.User;
import com.twitch.bot.twitch_connection.ChannelsData;
import com.twitch.bot.twitch_connection.Connection;
import com.twitch.bot.twitch_connection.Users;

@RestController
public class Controller {
    private static final Logger LOG = Logger.getLogger(Controller.class.getName());
    private Connection twitch_connection;
    private Users users;
    HttpHeaders responseHeaders = new HttpHeaders();
    
    public Controller(ChannelsData channelsData, Connection twitch_connection, Users users) throws Exception{
        this.twitch_connection = twitch_connection;
        this.users = users;
    }

    @GetMapping("/channels")
    public ResponseEntity<Object> getTwitchChannels() throws Exception {
        return new ResponseEntity<>(twitch_connection.getAllChannels(), HttpStatus.OK);
    }

    @GetMapping("/twitch_analysis")
    public ResponseEntity<Object> getTwitchAnalysisData(@RequestParam("channel_name") String channelName) throws Exception {
        HashMap<String, Object> response = new HashMap<>();
        response.put("twitch_analysis", twitch_connection.getTwitchAnalysisOfAChannel(channelName).toList().stream().map(m -> ((HashMap<String, Object>) m)).collect(Collectors.toList()));
        response.put("channel_name", channelName);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/channel_broadcastId")
    public ResponseEntity<Object> getChannelBroadcastId(@RequestParam("channel_name") String channelName) throws Exception {
        return new ResponseEntity<>(twitch_connection.getUserBroadcasterId(channelName), HttpStatus.OK);
    }

    @PostMapping("/addChannel")
    public ResponseEntity<Object> subscribeChannel(@RequestParam("channel_name") String channelName) throws Exception {
        twitch_connection.addAndJoinChannel(channelName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping("/removeChannel")
    public ResponseEntity<Object> unSubscribeChannel(@RequestParam("channel_name") String channelName) throws Exception {
        twitch_connection.removeAndDeleteChannelData(channelName);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("user/authenticate")
    public ResponseEntity<Object> authenticateUser(@RequestBody HashMap<String, String> credentials) {
        LOG.log(Level.INFO, "POST /user/authenticate {0}", new Object[] { credentials });
        HashMap<String, Object> response = new HashMap<>();
        try {
            if (!(credentials.containsKey("username") || credentials.containsKey("email"))
                    || !credentials.containsKey("password")) {
                        throw new IllegalArgumentException();
            }
            String userName = credentials.get("username");
            String email = credentials.get("email");
            String password = credentials.get("password");

            Boolean isValidUser = (userName != null) ? users.authenticateUser(userName, password, true) : users.authenticateUser(email, password, false);
            if (isValidUser) {
                User user = (userName != null) ? users.getUserDetails(userName, password, true)
                        : users.getUserDetails(email, password, false);
                response.put("user_name", user.getName());
                response.put("email", user.getEmail());
                response.put("user_id", user.getUserId());
                return new ResponseEntity<>(response, HttpStatus.OK);
            } else {
                return  new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }

            

        } catch (IllegalArgumentException ex) {
            LOG.log(Level.SEVERE, "INVALID_BODY");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getLocalizedMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("user/register")
    public ResponseEntity<Object> register(@RequestBody HashMap<String, String> credentials) {
        LOG.log(Level.INFO, "POST /user/register {0}", new Object[] { credentials });
        HashMap<String, Object> response = new HashMap<>();
        try {
            if (!credentials.containsKey("username") || !credentials.containsKey("email")
                    || !credentials.containsKey("password")) {
                throw new IllegalArgumentException();
            }
            String userName = credentials.get("username");
            String email = credentials.get("email");
            String password = credentials.get("password");

            User user = users.registerUser(userName, password, email);
            response.put("user_name", user.getName());
            response.put("email", user.getEmail());
            response.put("user_id", user.getUserId());
            return new ResponseEntity<>(response, HttpStatus.OK);
        } catch (IllegalArgumentException ex) {
            LOG.log(Level.SEVERE, "INVALID_BODY");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getLocalizedMessage());
            if (ex.getMessage() != null && ex.getMessage().equals("User Already Present")) {
                return new ResponseEntity<>(HttpStatus.NOT_ACCEPTABLE);
            } else {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }

    @GetMapping("user/subscriptions")
    public ResponseEntity<Object> getUserSubscriptions(@RequestHeader Object userId) {
        try {
            Boolean isValidUser = users.authenticateUser(Integer.parseInt(userId.toString()));
            if (isValidUser) {
                User user = users.getUserDetails(Integer.parseInt(userId.toString()));
                return new ResponseEntity<>(users.getUserSubscriptions(user), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getLocalizedMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("user/subscriptions")
    public ResponseEntity<Object> addUserSubscriptions(@RequestHeader Object userId, @RequestParam("channel_id") String channelId) {
        try {
            Subscriptions subscription = users.checkAndAddUserSubscriptions(Integer.parseInt(channelId.toString()), Integer.parseInt(channelId));
            if(subscription != null){
                return new ResponseEntity<>(subscription, HttpStatus.OK);
            }else{
                return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
            }
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getLocalizedMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}
