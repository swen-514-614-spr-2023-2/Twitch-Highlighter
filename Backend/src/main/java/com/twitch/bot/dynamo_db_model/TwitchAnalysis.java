package com.twitch.bot.dynamo_db_model;

import org.json.JSONObject;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName="Twitch_Analysis")
public class TwitchAnalysis {
    
    private Long id;
    private Long twitchChannelPk;
    private JSONObject sentimentalClipsCollection ;
    private Long timestamp;

    @DynamoDBHashKey(attributeName="id")
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @DynamoDBAttribute(attributeName="twitch_channel_pk")
    public Long getTwitchChannelPk() {
     return twitchChannelPk;
     }

     public void setTwitchChannelPk(Long twitchChannelPk) {
         this.twitchChannelPk = twitchChannelPk;
     }

     @DynamoDBAttribute(attributeName="sentimental_clips_collection")
     public JSONObject getSentimentalClipsCollection() {
         return sentimentalClipsCollection;
     }

     public void setSentimentalClipsCollection(JSONObject sentimentalClipsCollection) {
         this.sentimentalClipsCollection = sentimentalClipsCollection;
     }

    @DynamoDBAttribute(attributeName="timestamp")
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

}
