package com.twitch.bot.dynamo_db_model;

import org.json.JSONObject;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAutoGeneratedKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName="Twitch_Analysis")
public class TwitchAnalysis {
    
    private String id;
    private Long twitchChannelPk;
    private SentimentalData sentimentalClipsCollection ;
    private Long timestamp;

    @DynamoDBHashKey(attributeName="id")
    @DynamoDBAutoGeneratedKey
    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @DynamoDBAttribute(attributeName="twitchChannelPk")
    public Long getTwitchChannelPk() {
     return twitchChannelPk;
     }

     public void setTwitchChannelPk(Long twitchChannelPk) {
         this.twitchChannelPk = twitchChannelPk;
     }

     @DynamoDBAttribute(attributeName="sentimental_clips_collection")
     public SentimentalData getSentimentalClipsCollection() {
         return sentimentalClipsCollection;
     }

     public void setSentimentalClipsCollection(SentimentalData sentimentalClipsCollection) {
         this.sentimentalClipsCollection = sentimentalClipsCollection;
     }

    @DynamoDBRangeKey(attributeName="timestamp")
    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    @DynamoDBDocument
    public class SentimentalData{
        private String sentimental_analysis;
        public String getSentimental_analysis() {
            return sentimental_analysis;
        }
        public void setSentimental_analysis(String sentimental_analysis) {
            this.sentimental_analysis = sentimental_analysis;
        }
        public String getClip_details() {
            return clip_details;
        }
        public void setClip_details(String clip_details) {
            this.clip_details = clip_details;
        }
        private String clip_details;
    }

}
