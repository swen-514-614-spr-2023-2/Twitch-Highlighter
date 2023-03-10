package com.twitch.bot.controller;

import java.util.logging.Logger;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.twitch.bot.twitch_connection.Connection;

@RestController
public class Controller {
    private static final Logger LOG = Logger.getLogger(Controller.class.getName());
    private Connection twitch_connection;
    public Controller(Connection twitch_connection){
        this.twitch_connection = twitch_connection;
    }
    

    @GetMapping("/addChannel")
    public ResponseEntity<Object> subscribeChannel() throws Exception {
        twitch_connection.connect();
        twitch_connection.joinChannel("tubbo");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/removeChannel")
    public ResponseEntity<Object> unSubscribeChannel() throws Exception {
        twitch_connection.removeChannel("tubbo");
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @GetMapping("/clips")
    public ResponseEntity<Object> getClips() throws Exception {
        twitch_connection.getUsers();
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
