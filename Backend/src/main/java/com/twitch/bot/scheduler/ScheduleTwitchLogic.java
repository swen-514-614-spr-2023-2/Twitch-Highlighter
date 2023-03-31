package com.twitch.bot.scheduler;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehend.model.ComprehendException;
import software.amazon.awssdk.services.comprehend.model.DetectSentimentRequest;
import software.amazon.awssdk.services.comprehend.model.DetectSentimentResponse;

import com.twitch.bot.api.ApiHandler;
import com.twitch.bot.api.ApiHandler.PATH;
import com.twitch.bot.db_utils.TwitchData;
import com.twitch.bot.model.Channel;
import com.twitch.bot.twitch_connection.ChannelsData;

@Component
public class ScheduleTwitchLogic {
    private static final Logger LOG = Logger.getLogger(ScheduleTwitchLogic.class.getName());
    private static final long frequencySeconds = 15000l;
    private TwitchData twitchData;
    private ApiHandler apiHandler;
    private Double avg_user_comment = 0.01;

    public ScheduleTwitchLogic(TwitchData twitchData, ApiHandler apiHandler){
        this.twitchData = twitchData;
        this.apiHandler = apiHandler;
    }
    @Scheduled(fixedRate = 15000)
    public void jobRunner() throws Exception {
        Long currentTime = System.currentTimeMillis();
        LOG.log(Level.INFO, "currentTime In Schedule ::: " + currentTime);
        // twitchData = getTwitchDataInstance();
        // apiHandler = new ApiHandler(twitchData);
        List<Channel> allChannelNames = getChannelNames();
        Iterator<Channel> allChannelNamesIter = allChannelNames.iterator();
        while(allChannelNamesIter.hasNext()){
            Channel channel = allChannelNamesIter.next();
            if(channel.getIsListeningToChannel()){
                processChannelMessages(channel, currentTime);
            }
        }
    }

    public List<Channel> getChannelNames() {
        HashMap<String, Channel> data = ChannelsData.getChannels();
        if (null == data || data.size() == 0) {
            return twitchData.getChannelDetails();
        }else{
            return new ArrayList<>(data.values());
        }
    }

    public void processChannelMessages(Channel channel, Long tillTimeStamp) throws Exception{
        Long thresholdValue = getThresholdValueBasedOnChannel(channel);
        if(thresholdValue == -1){
            //channel.setIsListeningToChannel(false);
            twitchData.deleteTwitchMessageForChannel(channel, tillTimeStamp);
            return ;
        }
        JSONArray messages = twitchData.getTwitchMessageForChannel(channel, tillTimeStamp - frequencySeconds, tillTimeStamp);
        if(messages.length() >= thresholdValue){
            String sentimental_result = awsSentimentalAnalysis(messageMerge(messages));
            JSONObject clips = awsClipsGeneration(channel);
            twitchData.updateTwitchAnalysis(channel, sentimental_result, clips);
        }
        twitchData.deleteTwitchMessageForChannel(channel, tillTimeStamp);
    }

    public Long getThresholdValueBasedOnChannel(Channel channel) throws Exception{
        String response = apiHandler.setPath(PATH.GET_STREAMS).setParams(new JSONObject().put("user_login", channel.getChannelName())).setHeaders(new JSONObject().put("set_client_id", "Client-Id")).GET();
        JSONObject responseData = new JSONObject(response);
        if(responseData.isEmpty()){
            return -1l;
        }
        if(responseData.getJSONArray("data").isEmpty()){
            return -1l;
        }
        Long viewer_count = Long.valueOf(responseData.getJSONArray("data").getJSONObject(0).get("viewer_count").toString());
        Long avg_comment_multiplied_viewer_60sec = Math.round(viewer_count * avg_user_comment);
        Long avg_comment_multiplied_viewer_1sec = avg_comment_multiplied_viewer_60sec / 60;
        Long avg_comment_multiplied_viewer_frequencySeconds = (frequencySeconds / 1000) * avg_comment_multiplied_viewer_1sec;
        return avg_comment_multiplied_viewer_frequencySeconds;
    }

    // public TwitchData getTwitchDataInstance() throws Exception{
    //     LOG.log(Level.INFO, "Current Path In Schedule ::: " + System.getProperty("user.dir"));
    //     String nonAwsPath = "src/main/resources/application.properties";
    //     String awsPath = "../../../../../resources//application.properties";
    //     try (InputStream input = new FileInputStream("src/main/resources/application.properties")) {
    //         Properties prop = new Properties();
    //         prop.load(input);
    //         return new TwitchData(prop.getProperty("mongodb.password"), false, null);
    //     } catch (Exception ex) {
    //         LOG.log(Level.SEVERE, "Exception in loading properties file ::: ", ex);
    //         throw ex;
    //     }
    // }

    public String awsSentimentalAnalysis(String messageText){
       
        Region region = Region.US_EAST_1;
        
        if(!TwitchData.isAwsEnvironment()){
            JSONObject credentials = twitchData.getCloudCredentials();
            System.setProperty("aws.accessKeyId",credentials.getString("access_id"));
            System.setProperty("aws.secretAccessKey",credentials.getString("access_key"));
        }else{
            System.setProperty("aws.accessKeyId", System.getenv("access_id"));
            System.setProperty("aws.secretAccessKey", System.getenv("access_key"));
        }
       

        ComprehendClient comClient = ComprehendClient.builder()
            .region(region).credentialsProvider(SystemPropertyCredentialsProvider.create())
            .build();

        try {
            DetectSentimentRequest detectSentimentRequest = DetectSentimentRequest.builder()
                .text(messageText)
                .languageCode("en")
                .build();

            DetectSentimentResponse detectSentimentResult = comClient.detectSentiment(detectSentimentRequest);
            return detectSentimentResult.sentimentAsString();

        } catch (ComprehendException ex) {
            LOG.log(Level.WARNING, "Exception is ::: ", ex);
            return "Exception";
        }
    }

    public String messageMerge(JSONArray messages){
        String messagesStr = "";
        Iterator<Object> messagesIter = messages.iterator();
        while(messagesIter.hasNext()){
            JSONObject messageObj = (JSONObject)messagesIter.next();
            messagesStr += messageObj.get("message").toString();
        }
        return messagesStr;
    }

    public JSONObject awsClipsGeneration(Channel channel) throws Exception{
        JSONObject data = new JSONObject();
        String response = apiHandler.setPath(PATH.CLIPS).setParams(new JSONObject().put("broadcaster_id", channel.getTwitchId())).setHeaders(new JSONObject().put("set_client_id", "Client-Id")).POST();
        JSONObject responseData = new JSONObject(response);
        String clip_id = responseData.getJSONArray("data").getJSONObject(0).getString("id");
        Thread.sleep(500);//*Thread Sleeps so that the create clip is done generating on twitch side */
        response = apiHandler.setPath(PATH.CLIPS).setParams(new JSONObject().put("id", clip_id)).setHeaders(new JSONObject().put("set_client_id", "Client-Id")).GET();
        responseData = new JSONObject(response);
        responseData = responseData.getJSONArray("data").getJSONObject(0);
        data.put("clip_id", clip_id);
        data.put("video_url", responseData.get("url").toString());
        data.put("embed_url", responseData.get("embed_url").toString());
        data.put("created_at", responseData.get("created_at").toString());
        data.put("thumbnail_url", responseData.get("thumbnail_url").toString()); 
        return data;
    }
}
