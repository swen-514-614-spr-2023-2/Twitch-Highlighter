package com.twitch.bot.model;

public class User {
    private String userName;

    public User(String userName){
        this.userName = userName;
    }

    public String getUser(){
        return this.userName;
    }
}
