package com.twitch.bot.db_utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.model.AmazonDynamoDBException;
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
import com.twitch.bot.dynamo_db_model.Messages;
import com.twitch.bot.dynamo_db_model.TwitchAnalysis;
import com.twitch.bot.model.Channel;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lte;

@Component
public class TwitchData {
    private static final Logger LOG = Logger.getLogger(TwitchData.class.getName());
    MongoClient mongoClient;
    AmazonDynamoDB dynamoDb;
    private static String dbPassword;

    public enum DYNAMODB_TABLES {
        MESSAGES("Messages"),
        TWITCH_ANALYSIS("Twitch_Analysis");

        String tableName;

        DYNAMODB_TABLES(String tableName) {
            this.tableName = tableName;
        }

        @Override
        public String toString() {
            return this.tableName;
        }
    }

    public TwitchData(@Value("${mongodb.password}") String dbPassword, @Value("true") Boolean isCalledOnInitalize,
            @Value("${dynamodb.names}") String dynamoDbNames) {
        if (isAwsEnvironment()) {
            LOG.log(Level.INFO, "inside AWS Environment");
            makeConnectionToDynamoDB(getDyanmoDbTables());
        } else {
            LOG.log(Level.INFO, "outside AWS Environment");
            if (isCalledOnInitalize) {
                setDbPassword(dbPassword);
            }
            makeConnectionToDB();
        }
    }

    public static void setDbPassword(String dbPassword) {
        TwitchData.dbPassword = dbPassword;
    }

    public static Boolean isAwsEnvironment() {
        return System.getenv("AWS_ENVIRONMENT") != null ? Boolean.valueOf(System.getenv("AWS_ENVIRONMENT").toString())
                : false;
    }

    private static List<String> getDyanmoDbTables() {
        List<String> tableNames = new ArrayList<>();
        tableNames.add(DYNAMODB_TABLES.MESSAGES.toString());
        tableNames.add(DYNAMODB_TABLES.TWITCH_ANALYSIS.toString());
        return tableNames;
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
    }

    public void makeConnectionToDynamoDB(List<String> dynamoDbNames) {
        LOG.log(Level.INFO, "Cloud Credentials ::: " + getCloudCredentialsFromAWS().toString());
        dynamoDb = AmazonDynamoDBClientBuilder.defaultClient();
        ListTablesRequest request;

        boolean more_tables = true;
        String last_name = null;

        while (more_tables) {
            try {
                if (last_name == null) {
                    request = new ListTablesRequest().withLimit(10);
                } else {
                    request = new ListTablesRequest()
                            .withLimit(10)
                            .withExclusiveStartTableName(last_name);
                }

                ListTablesResult table_list = dynamoDb.listTables(request);
                List<String> table_names = table_list.getTableNames();

                if (table_names.size() > 0) {
                    for (String cur_name : table_names) {
                        if (dynamoDbNames.contains(cur_name)) {
                            LOG.log(Level.INFO, "Table " + cur_name + " Exists");
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
        if (!dynamoDbNames.isEmpty()) {
            LOG.log(Level.SEVERE, "Tables Not Found ::: ", dynamoDbNames.toString());
            for (String tableName : dynamoDbNames) {
                LOG.log(Level.INFO, "Creating Table " + tableName);
                createTableInDyanmoDB(tableName);
            }
        }
    }

    public void createTableInDyanmoDB(String tableName) {
        CreateTableRequest request = new CreateTableRequest().withTableName(tableName)
                .withKeySchema(new KeySchemaElement().withAttributeName("id").withKeyType(KeyType.HASH))
                .withProvisionedThroughput(new ProvisionedThroughput().withReadCapacityUnits(5L)
                        .withWriteCapacityUnits(5L))
                .withAttributeDefinitions(new AttributeDefinition("id", ScalarAttributeType.N));

        dynamoDb.createTable(request);
    }

    public JSONObject getTwitchCredentials() {
        return getTwitchCredentialsFromMongoDB();
    }

    private JSONObject getTwitchCredentialsFromMongoDB() {
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
        return setTwitchCredentialsToMongoDB(data);
    }

    private Boolean setTwitchCredentialsToMongoDB(JSONObject data) {
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
        if (isAwsEnvironment()) {
            addTwitchMessageToDynamoDB(user, channel, message, timeStamp);
        } else {
            addTwitchMessageToMongoDB(user, channel, message, timeStamp);
        }
    }

    private void addTwitchMessageToMongoDB(String user, Channel channel, String message, Long timeStamp) {
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("messages");
        Document document = new Document("channel_name", channel.getChannelName())
                .append("user_name", user.toString())
                .append("message", message)
                .append("time_stamp", timeStamp);
        collection.insertOne(document);
    }

    private void addTwitchMessageToDynamoDB(String user, Channel channel, String message, Long timeStamp) {

        Messages messages = new Messages();
        try {
            messages.setChannelName(channel.getChannelName());
            messages.setMessage(message);
            messages.setTimestamp(timeStamp);
            messages.setUserName(user.toString());

            DynamoDBMapper mapper = new DynamoDBMapper(dynamoDb);
            mapper.save(messages);
            LOG.log(Level.INFO, "Twitch Message Added in Dynamo DB");
        } catch (AmazonDynamoDBException ex) {
            LOG.log(Level.SEVERE, "Exception In Adding Twitch Message ::: " + ex);
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
        if (isAwsEnvironment()) {
            Messages message = new Messages();
            message.setId(null);
            message.setChannelName(channel.getChannelName());
            message.setMessage(null);
            message.setUserName(user);
            message.setTimestamp(null);
            return getTwitchMessageForChannelFromDynamoDBInJSONFormat(null, fromTimeStamp, toTimeStamp);
        } else {
            return getTwitchMessageForChannelFromMongoDB(channel, user, fromTimeStamp, toTimeStamp);
        }
    }

    private JSONArray getTwitchMessageForChannelFromMongoDB(Channel channel, String user, Long fromTimeStamp,
            Long toTimeStamp) {
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

    private JSONArray getTwitchMessageForChannelFromDynamoDBInJSONFormat(Messages message, Long fromTimeStamp,
            Long toTimeStamp) {
        return new JSONArray(getTwitchMessageForChannelFromDynamoDB(message, fromTimeStamp, toTimeStamp));
    }

    private List<Messages> getTwitchMessageForChannelFromDynamoDB(Messages message, Long fromTimeStamp,
            Long toTimeStamp) {
        DynamoDBMapper mapper = new DynamoDBMapper(dynamoDb);
        String expression = "";
        Map<String, AttributeValue> expressionValue = new HashMap<String, AttributeValue>();
        if (message.getId() != null) {
            expression += "id = :v1";
            expressionValue.put(":v1", new AttributeValue().withN(message.getId().toString()));
        }
        if (message.getChannelName() != null) {
            if (expression.trim() != "") {
                expression += " and ";
            }
            expression += "channel_name = :v2";
            expressionValue.put(":v2", new AttributeValue().withS(message.getChannelName()));
        }
        if (message.getUserName() != null) {
            if (expression.trim() != "") {
                expression += " and ";
            }
            expression += "user_name = :v3";
            expressionValue.put(":v3", new AttributeValue().withS(message.getUserName()));
        }
        if (message.getMessage() != null) {
            if (expression.trim() != "") {
                expression += " and ";
            }
            expression += "message = :v4";
            expressionValue.put(":v4", new AttributeValue().withS(message.getMessage()));
        }
        if (message.getTimestamp() != null) {
            if (expression.trim() != "") {
                expression += " and ";
            }
            expression += "timestamp = :v5";
            expressionValue.put(":v5", new AttributeValue().withN(message.getTimestamp().toString()));
        } else {
            if (fromTimeStamp != null && toTimeStamp != null) {
                expression += "timestamp between :v5 and :v6";
                expressionValue.put(":v5", new AttributeValue().withN(fromTimeStamp.toString()));
                expressionValue.put(":v6", new AttributeValue().withN(toTimeStamp.toString()));
            } else if (fromTimeStamp != null) {
                expression += "timestamp >= :v5";
                expressionValue.put(":v5", new AttributeValue().withN(fromTimeStamp.toString()));
            } else if (toTimeStamp != null) {
                expression += "timestamp <= :v5";
                expressionValue.put(":v5", new AttributeValue().withN(toTimeStamp.toString()));
            }
        }

        if (expression.trim() == "") {
            return new ArrayList<Messages>();
        }
        DynamoDBQueryExpression<Messages> queryExpression = new DynamoDBQueryExpression<Messages>()
                .withKeyConditionExpression(expression)
                .withExpressionAttributeValues(expressionValue);

        List<Messages> result = mapper.query(Messages.class, queryExpression);
        LOG.log(Level.INFO, "twitch Message Fetched ::: " + Arrays.toString(result.toArray()) );
        return result;
    }

    public void deleteTwitchMessageForChannel(Channel channel) {
        deleteTwitchMessageForChannel(channel, null, null);
    }

    public void deleteTwitchMessageForChannel(Channel channel, Long timeStamp) {
        deleteTwitchMessageForChannel(channel, null, timeStamp);
    }

    public void deleteTwitchMessageForChannel(Channel channel, String user) {
        deleteTwitchMessageForChannel(channel, user, null);
    }

    public void deleteTwitchMessageForChannel(Channel channel, String user, Long toTimeStamp) {
        if (isAwsEnvironment()) {
            Messages message = new Messages();
            message.setId(null);
            message.setChannelName(channel.getChannelName());
            message.setMessage(null);
            message.setUserName(user);
            message.setTimestamp(null);
            deleteTwitchMessageForChannelFromDynamoDB(message, null, toTimeStamp);
        } else {
            deleteTwitchMessageForChannelFromMongoDB(channel, user, toTimeStamp);
        }
    }

    private void deleteTwitchMessageForChannelFromMongoDB(Channel channel, String user, Long toTimeStamp) {
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("messages");
        Bson filter = eq("channel_name", channel.getChannelName());
        if (null != toTimeStamp) {
            filter = lte("time_stamp", toTimeStamp);
        }
        if (null != user) {
            filter = eq("user_name", user);
        }
        collection.deleteMany(filter);
    }

    private void deleteTwitchMessageForChannelFromDynamoDB(Messages message, Long fromTimeStamp, Long toTimeStamp) {
        DynamoDBMapper mapper = new DynamoDBMapper(dynamoDb);
        mapper.batchDelete(getTwitchMessageForChannelFromDynamoDB(message, fromTimeStamp, toTimeStamp));
        LOG.log(Level.INFO, "twitch Messages Deleted");
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

    public Long getNextChannelId() {
        List<Channel> channels = getChannelDetails();
        Iterator<Channel> channelsIter = channels.iterator();
        Long id = 0l;
        while (channelsIter.hasNext()) {
            Channel channel = channelsIter.next();
            if (id < Long.valueOf(channel.getId())) {
                id = Long.valueOf(channel.getId());
            }
        }
        return id + 1;
    }

    public Channel addChannelDetails(String channelName, String channelId) {
        if (channelName == null || channelId == null || channelName.trim() == "" || channelId.trim() == "") {
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

    public void deleteChannelDetails(Integer id) {
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

    public JSONArray getTwitchAnalysisOfAChannel(Channel channel) {
        if(isAwsEnvironment()){
            return getTwitchAnalysisOfAChannelFromDynamoDBInJSON(channel);
        }else{
            return getTwitchAnalysisOfAChannelFromMongoDB(channel);
        }
    }

    private JSONArray getTwitchAnalysisOfAChannelFromMongoDB(Channel channel) {
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
            arrayData.put(new JSONObject(data.get("sentimental_clips_collection").toString()));
        }
        return arrayData;
    }

    private JSONArray getTwitchAnalysisOfAChannelFromDynamoDBInJSON(Channel channel) {
        List<TwitchAnalysis> data = getTwitchAnalysisOfAChannelFromDynamoDB(channel);
        JSONArray result = new JSONArray();
        Iterator<TwitchAnalysis> dataIter = data.iterator();
        while(dataIter.hasNext()){
            TwitchAnalysis twitchAnalysis = dataIter.next();
            result.put(twitchAnalysis.getSentimentalClipsCollection());
        }
        return result;
    }

    private List<TwitchAnalysis> getTwitchAnalysisOfAChannelFromDynamoDB(Channel channel) {
        DynamoDBMapper mapper = new DynamoDBMapper(dynamoDb);
        String expression = "";
        Map<String, AttributeValue> expressionValue = new HashMap<String, AttributeValue>();
        expression += "id = :v1";
        expressionValue.put(":v1", new AttributeValue().withN(channel.getId().toString()));

        DynamoDBQueryExpression<TwitchAnalysis> queryExpression = new DynamoDBQueryExpression<TwitchAnalysis>()
                .withKeyConditionExpression(expression)
                .withExpressionAttributeValues(expressionValue);

        List<TwitchAnalysis> result = mapper.query(TwitchAnalysis.class, queryExpression);
        LOG.log(Level.INFO, "twitch Analysis Fetched ::: " + Arrays.toString(result.toArray()) );
        return result;
    }

    public void addTwitchAnalysis(Channel channel, String sentimental_result, JSONObject clip_details, Long timeStamp) {
        if(timeStamp == null){
            timeStamp = System.currentTimeMillis();
        }
        if(isAwsEnvironment()){
            addTwitchAnalysisInDynamoDB(channel, sentimental_result, clip_details, timeStamp);
        }else{
            addTwitchAnalysisInMongoDB(channel, sentimental_result, clip_details, timeStamp);
        }
    }

    private void addTwitchAnalysisInMongoDB(Channel channel, String sentimental_result, JSONObject clip_details, Long timeStamp) {
        MongoDatabase database = mongoClient.getDatabase("twitch");
        MongoCollection<Document> collection = database.getCollection("twitch_analysis");
        Document document = new Document("twitch_channel_id", channel.getTwitchId())
                .append("sentimental_clips_collection", new JSONArray().put(
                        new JSONObject()
                                .put("sentimental_analysis", sentimental_result)
                                .put("clip_details", clip_details))
                        .toString())
                .append("time_stamp", timeStamp);
        collection.insertOne(document);
    }

    private void addTwitchAnalysisInDynamoDB(Channel channel, String sentimental_result, JSONObject clip_details, Long timeStamp) {
        TwitchAnalysis twitchAnalysis = new TwitchAnalysis();
        try {
            twitchAnalysis.setSentimentalClipsCollection( new JSONObject()
            .put("sentimental_analysis", sentimental_result)
            .put("clip_details", clip_details)
            );
            twitchAnalysis.setTwitchChannelPk(Long.valueOf(channel.getId().toString()));
            twitchAnalysis.setTimestamp(timeStamp);

            DynamoDBMapper mapper = new DynamoDBMapper(dynamoDb);
            mapper.save(twitchAnalysis);
            LOG.log(Level.INFO, "Twitch Analysis Added");
        } catch (AmazonDynamoDBException ex) {
            LOG.log(Level.SEVERE, "Exception ::: " + ex);
        }
    }

    public JSONObject getCloudCredentials() {
        if(isAwsEnvironment()){
            return getCloudCredentialsFromAWS();
        }else{
            return getCloudCredentialsFromMongoDB();
        }
    }

    private JSONObject getCloudCredentialsFromMongoDB() {
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

    private JSONObject getCloudCredentialsFromAWS() {
        return new JSONObject().put("access_id", System.getenv("access_id")).put("access_key", System.getenv("access_key"));
    }
}
