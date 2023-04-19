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
    public static class SentimentalData{
        private String sentimental_analysis;
        private ClipsDetails clip_details;

        public String getSentimental_analysis() {
            return sentimental_analysis;
        }
        public void setSentimental_analysis(String sentimental_analysis) {
            this.sentimental_analysis = sentimental_analysis;
        }
        public ClipsDetails getClip_details() {
            return clip_details;
        }
        public void setClip_details(ClipsDetails clip_details) {
            this.clip_details = clip_details;
        }

        public SentimentalData(){
            
        }
    }

    @DynamoDBDocument
    public static class ClipsDetails{
        String clip_id;
        String video_url;
        String embed_url;
        String created_at;
        String thumbnail_url;

        public String getClip_id() {
            return clip_id;
        }

        public void setClip_id(String clip_id) {
            this.clip_id = clip_id;
        }

        public String getVideo_url() {
            return video_url;
        }

        public void setVideo_url(String video_url) {
            this.video_url = video_url;
        }

        public String getEmbed_url() {
            return embed_url;
        }

        public void setEmbed_url(String embed_url) {
            this.embed_url = embed_url;
        }

        public String getCreated_at() {
            return created_at;
        }

        public void setCreated_at(String created_at) {
            this.created_at = created_at;
        }

        public String getThumbnail_url() {
            return thumbnail_url;
        }

        public void setThumbnail_url(String thumbnail_url) {
            this.thumbnail_url = thumbnail_url;
        }

        public ClipsDetails(){
            
        }

        @Override
        public String toString(){
            return "[ " + "clip_id = " + clip_id + ", video_url = " + video_url + ", embed_url = " + embed_url + ", created_at = " + created_at + ", thumbnail_url = " + thumbnail_url + " ]";
        }
    }

}
