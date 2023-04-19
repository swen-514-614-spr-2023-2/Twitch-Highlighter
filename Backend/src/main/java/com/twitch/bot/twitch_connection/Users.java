package com.twitch.bot.twitch_connection;

import java.util.List;

import org.springframework.stereotype.Component;

import com.twitch.bot.db_utils.TwitchAWS_RDS;
import com.twitch.bot.model.Channel;
import com.twitch.bot.model.Subscriptions;
import com.twitch.bot.model.User;

@Component
public class Users {
    TwitchAWS_RDS twitchAWS_RDS;
    public Users(TwitchAWS_RDS twitchAWS_RDS){
        this.twitchAWS_RDS = twitchAWS_RDS;
    }

    public Boolean authenticateUser(String username, String password, Boolean isUserName) throws Exception{
        User user = twitchAWS_RDS.getUserDetails(username, password, isUserName);
        return (user != null);
    }

    public Boolean checkIfEmailOrUserNamePresent(String name, Boolean isUserName) throws Exception{
        User user = twitchAWS_RDS.getUserDetails(name, isUserName);
        return (user != null);
    }

    public Boolean authenticateUser(Integer userId) throws Exception{
        User user = twitchAWS_RDS.getUserDetails(userId);
        return (user != null);
    }

    public User getUserDetails(String username, String password, Boolean isUserName) throws Exception{
        return twitchAWS_RDS.getUserDetails(username, password, isUserName);
    }

    public User getUserDetails(Integer userId) throws Exception{
        return twitchAWS_RDS.getUserDetails(userId);
    }

    public User registerUser(String username, String password, String email) throws Exception{
        if(!checkIfEmailOrUserNamePresent(email, false)){
            return twitchAWS_RDS.addUserDetails(username, email, password);
        }else{
            throw new Exception("User Already Present");
        }
    }

    public List<Subscriptions> getUserSubscriptions(User user) throws Exception{
        return twitchAWS_RDS.getSubscriptionDetailsBasedOnUserOrSubscriptionId(user.getUserId(), true); 
    }

    public Subscriptions checkAndAddUserSubscriptions(Integer userId, Integer channelId) throws Exception{
        if(!authenticateUser(userId)){
            Channel channel = ChannelsData.getChannel(channelId);
            if(channel != null){
                return addUserSubscriptions(getUserDetails(userId), channel);
            }
        }
        return null;
    }

    public Subscriptions addUserSubscriptions(User user, Channel channel) throws Exception{
        if(twitchAWS_RDS.checkIfSubscriptionExists(user.getUserId(), channel.getId())){
            throw new Exception("Subscription Already Present");
        }else{
            return twitchAWS_RDS.addSubscriptionDetails(user, channel);
        }
    }
}
