package com.twitch.bot.dynamo_db_model;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

@DynamoDBTable(tableName="Messages")
public class Messages {

       private Long id;
       private String channelName;
       private String userName;
       private String message;
       private Long timestamp;

       @DynamoDBHashKey(attributeName="id")
       public Long getId() {
           return this.id;
       }

       public void setId(Long id) {
           this.id = id;
       }

       @DynamoDBAttribute(attributeName="channel_name")
       public String getChannelName() {
        return channelName;
        }

        public void setChannelName(String channelName) {
            this.channelName = channelName;
        }

        @DynamoDBAttribute(attributeName="user_name")
        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        @DynamoDBAttribute(attributeName="message")
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        @DynamoDBAttribute(attributeName="timestamp")
        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
   }