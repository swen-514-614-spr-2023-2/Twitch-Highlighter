package com.twitch.bot.db_utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;
import com.mongodb.BasicDBObject;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Updates;
import com.mongodb.util.JSON;
import com.twitch.bot.model.Channel;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lte;

@Component
public class TwitchData {
    private static final Logger LOG = Logger.getLogger(TwitchData.class.getName());
    MongoClient mongoClient;
    AmazonDynamoDB dynamoDb;
    private static String dbPassword;

    public TwitchData(@Value("${mongodb.password}") String dbPassword, @Value("true") Boolean isCalledOnInitalize, @Value("${dynamodb.names}") String dynamoDbNames) {
        if(isAwsEnvironment()){
            AmazonDynamoDB ddb = AmazonDynamoDBClientBuilder.defaultClient();
        }else{
            if (isCalledOnInitalize) {
                setDbPassword(dbPassword);
            }
            makeConnectionToDB();
        }
    }

    public static void setDbPassword(String dbPassword) {
        TwitchData.dbPassword = dbPassword;
    }

    public static Boolean isAwsEnvironment(){
        return false;
        //return System.getenv("AWS_ENVIRONMENT") != null ? Boolean.valueOf(System.getenv("AWS_ENVIRONMENT").toString()) : false;
    }

    public void makeConnectionToDB() {
        String connectionData = "mongodb+srv://twitch:"
                + dbPassword
                + "@twitchprojectoauth.lfctwou.mongodb.net/?retryWrites=true&w=majority";
        ConnectionString connectionString = new ConnectionString(connectionData);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build();
        mongoClient = MongoClients.create(settings);
        Logger mongoLogger = Logger.getLogger("org.mongodb.driver");
        mongoLogger.setLevel(Level.OFF);
    }

    public void makeConnectionToDynamoDB(List<String> dynamoDbNames) {
        dynamoDb = AmazonDynamoDBClientBuilder.defaultClient();
        ListTablesRequest request;

        boolean more_tables = true;
        String last_name = null;

        while(more_tables) {
            try {
                if (last_name == null) {
                	request = new ListTablesRequest().withLimit(10);
                }
                else {
                	request = new ListTablesRequest()
                			.withLimit(10)
                			.withExclusiveStartTableName(last_name);
                }

                ListTablesResult table_list = dynamoDb.listTables(request);
                List<String> table_names = table_list.getTableNames();

                if (table_names.size() > 0) {
                    for (String cur_name : table_names) {
                        if(dynamoDbNames.contains(cur_name)){
                            dynamoDbNames.remove(cur_name);
                        }
                    }
                } else {
                    System.out.println("No tables found!");
                    System.exit(0);
                }

                last_name = table_list.getLastEvaluatedTableName();
                if (last_name == null) {
                    more_tables = false;
                }

            } catch (AmazonServiceException ex) {
                LOG.log(Level.SEVERE, "Exception in fetching tables ::: ", ex.getMessage());
            }
        }
        if(!dynamoDbNames.isEmpty()){
            LOG.log(Level.SEVERE, "Tables Not Found ::: ", dynamoDbNames.toString());
            for (String tableName : dynamoDbNames) {
                createTableInDyanmoDB(tableName);
            }
        }
    }

    public void createTableInDyanmoDB(String tableName) {
        CreateTableRequest request = new CreateTableRequest().withTableName(tableName).withKeySchema(new KeySchemaElement().withAttributeName("id").withKeyType(KeyType.HASH))
                .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(5L)
                    .withWriteCapacityUnits(5L)).withAttributeDefinitions(new AttributeDefinition("id", ScalarAttributeType.N));

        dynamoDb.createTable(request);
    }

    public JSONObject getTwitchCredentials() {
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("credentials");
        JSONObject data = new JSONObject();
        FindIterable<Document> iterDoc = collection.find();
        Iterator<Document> it = iterDoc.iterator();
        while (it.hasNext()) {
            Document document = it.next();
            JSONObject documentData = new JSONObject(document.toJson());
            data.put("access_token", documentData.getString("access_token"));
            data.put("refresh_token", documentData.getString("refresh_token"));
            data.put("client_id", documentData.getString("client_id"));
            data.put("client_secret", documentData.getString("client_secret"));
            data.put("user_name", documentData.getString("user_name"));
        }
        return data;
    }

    public Boolean setTwitchCredentials(JSONObject data) {
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("credentials");
        if (!data.has("client_id")) {
            LOG.info("Client Id not present in given data ::: " + data.toString());
            return false;
        }
        if (data.has("access_token")) {
            collection.updateOne(eq("client_id", data.get("client_id").toString()),
                    Updates.set("access_token", data.get("access_token").toString()));
        }
        if (data.has("refresh_token")) {
            collection.updateOne(eq("client_id", data.get("client_id").toString()),
                    Updates.set("refresh_token", data.get("refresh_token").toString()));
        }
        return true;
    }

    public void addTwitchMessage(String user, Channel channel, String message, Long timeStamp) {
        if (null == timeStamp) {
            timeStamp = System.currentTimeMillis();
        }
        if(isAwsEnvironment()){
            HashMap<String,AttributeValue> item_values = new HashMap<String,AttributeValue>();
            item_values.put("channel_name", new AttributeValue(channel.getChannelName()));
            item_values.put("user_name", new AttributeValue(user.toString()));
            item_values.put("message", new AttributeValue(message));
            AttributeValue attrVal =  new AttributeValue();
            attrVal.setN(timeStamp.toString());
            item_values.put("time_stamp", attrVal);
            dynamoDb.putItem("messages", item_values);

        }else{
            MongoDatabase database = mongoClient.getDatabase("twitch");
            MongoCollection<Document> collection = database.getCollection("messages");
            Document document = new Document("channel_name", channel.getChannelName())
                    .append("user_name", user.toString())
                    .append("message", message)
                    .append("time_stamp", timeStamp);
            collection.insertOne(document);
        }  
    }

    public JSONArray getTwitchMessageForChannel(Channel channel) {
        return getTwitchMessageForChannel(channel, null, null, null);
    }

    public JSONArray getTwitchMessageForChannel(Channel channel, String user) {
        return getTwitchMessageForChannel(channel, user, null, null);
    }

    public JSONArray getTwitchMessageForChannel(Channel channel, Long fromTimeStamp, Long toTimeStamp) {
        return getTwitchMessageForChannel(channel, null, fromTimeStamp, toTimeStamp);
    }

    public JSONArray getTwitchMessageForChannel(Channel channel, String user, Long fromTimeStamp, Long toTimeStamp) {
        JSONArray result = new JSONArray();
        BasicDBObject timeStampQuery = new BasicDBObject();
        if (null != fromTimeStamp) {
            timeStampQuery.append("$gte", fromTimeStamp);
        }
        if (null != toTimeStamp) {
            timeStampQuery.append("$lte", toTimeStamp);
        }
        BasicDBObject criteriaQuery = new BasicDBObject();
        criteriaQuery.append("channel_name", channel.getChannelName());
        if (null != user) {
            criteriaQuery.append("user_name", user);
        }
        if (!timeStampQuery.isEmpty()) {
            criteriaQuery.append("time_stamp", timeStampQuery);
        }
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("messages");
        FindIterable<Document> iterDoc = collection.find(criteriaQuery);
        Iterator<Document> it = iterDoc.iterator();
        while (it.hasNext()) {
            Document document = it.next();
            JSONObject data = new JSONObject(document.toJson());
            data.remove("_id");
            result.put(data);
        }
        return result;
    }

    public void deleteTwitchMessageForChannel(Channel channel){
        deleteTwitchMessageForChannel(channel, null, null);
    }

    public void deleteTwitchMessageForChannel(Channel channel, Long timeStamp){
        deleteTwitchMessageForChannel(channel, null, timeStamp);
    }

    public void deleteTwitchMessageForChannel(Channel channel, String user){
        deleteTwitchMessageForChannel(channel, user, null);
    }

    public void deleteTwitchMessageForChannel(Channel channel, String user, Long toTimeStamp) {
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("messages");
        Bson filter = eq("channel_name", channel.getChannelName());
        if(null != toTimeStamp){
            filter = lte("time_stamp", toTimeStamp);
        }
        if(null != user){
            filter = eq("user_name", user);
        }
        collection.deleteMany(filter);
    }

    public List<Channel> getChannelDetails() {
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("channels");
        List<Channel> data = new ArrayList<>();
        FindIterable<Document> iterDoc = collection.find();
        Iterator<Document> it = iterDoc.iterator();
        while (it.hasNext()) {
            Document document = it.next();
            JSONObject documentData = new JSONObject(document.toJson());
            data.add(new Channel(Integer.parseInt(documentData.getJSONObject("id").get("$numberLong").toString()),
                    documentData.get("name").toString(), documentData.get("twitch_id").toString(),
                    Boolean.valueOf(documentData.get("is_server_listening").toString())));
        }
        return data;
    }

    public Long getNextChannelId(){
        List<Channel> channels = getChannelDetails();
        Iterator<Channel> channelsIter = channels.iterator();
        Long id = 0l;
        while(channelsIter.hasNext()){
            Channel channel = channelsIter.next();
            if(id < Long.valueOf(channel.getId())){
                id = Long.valueOf(channel.getId());
            }
        }
        return id + 1;
    }

    public Channel addChannelDetails(String channelName, String channelId){
        if(channelName == null || channelId == null || channelName.trim() == "" || channelId.trim() == ""){
            return null;
        }
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("channels");
        Long id = getNextChannelId();
        Document document = new Document("id", id)
        .append("name", channelName)
        .append("twitch_id", channelId)
        .append("is_server_listening", false);
        collection.insertOne(document);

        Channel updatedChannel = new Channel(Integer.parseInt(id.toString()), channelName, channelId);
        return updatedChannel;
    }

    public void deleteChannelDetails(Integer id){
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("channels");
        Bson filter = eq("id", id);
        collection.deleteMany(filter);
    }

    public void updateChannelServerListeningData(Boolean isServerListening, String channelName) {
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("channels");
        collection.updateOne(eq("name", channelName),
                Updates.set("is_server_listening", isServerListening));
    }

    public JSONArray getTwitchAnalysisOfAChannel(Channel channel){
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("twitch_analysis");
        BasicDBObject criteriaQuery = new BasicDBObject();
        criteriaQuery.append("twitch_channel_id", channel.getTwitchId());
        FindIterable<Document> iterDoc = collection.find(criteriaQuery);
        Iterator<Document> it = iterDoc.iterator();
        JSONArray arrayData = new JSONArray();
        while (it.hasNext()) {
            Document document = it.next();
            JSONObject data = new JSONObject(document.toJson());
            arrayData.putAll(new JSONArray(data.get("sentimental_clips_collection").toString()));
        }
        return arrayData;
    }

    public void updateTwitchAnalysis(Channel channel, String sentimental_result, JSONObject clip_details) {
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("twitch_analysis");
        Boolean isExistingDataPresent = false;
        FindIterable<Document> iterDoc = collection.find();
        Iterator<Document> it = iterDoc.iterator();
        while (it.hasNext()) {
            Document document = it.next();
            JSONObject documentData = new JSONObject(document.toJson());
            if (documentData.get("twitch_channel_id") == Long.valueOf(channel.getTwitchId())) {
                isExistingDataPresent = true;
                JSONArray aws_analysis = documentData.getJSONArray("sentimental_clips_collection");
                aws_analysis.put(new JSONObject().put("sentimental_analysis", sentimental_result).put("clip_details",
                        clip_details));
                documentData.put("sentimental_clips_collection", aws_analysis);
                collection.updateOne(eq("twitch_channel_id", channel.getTwitchId()),
                        Updates.set("sentimental_clips_collection", documentData));
            }
        }
        if (!isExistingDataPresent) {
            Document document = new Document("twitch_channel_id", channel.getTwitchId())
                    .append("sentimental_clips_collection", new JSONArray().put(
                            new JSONObject()
                                    .put("sentimental_analysis", sentimental_result)
                                    .put("clip_details", clip_details)).toString()
                                    );
            collection.insertOne(document);
        }
    }

    public JSONObject getCloudCredentials(){
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("Cloud");
         FindIterable<Document> iterDoc = collection.find();
        Iterator<Document> it = iterDoc.iterator();
        JSONObject data = new JSONObject();
        while (it.hasNext()) {
            Document document = it.next();
            JSONObject documentData = new JSONObject(document.toJson());
            data.put("access_id", documentData.get("id").toString());
            data.put("access_key", documentData.get("key").toString());
        }
        return data;
    }
}
