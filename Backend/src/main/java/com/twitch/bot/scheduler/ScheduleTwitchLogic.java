package com.twitch.bot.scheduler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehend.model.ComprehendException;
import software.amazon.awssdk.services.comprehend.model.DetectSentimentRequest;
import software.amazon.awssdk.services.comprehend.model.DetectSentimentResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import com.twitch.bot.api.ApiHandler;
import com.twitch.bot.api.ApiHandler.PATH;
import com.twitch.bot.db_utils.TwitchData;
import com.twitch.bot.dynamo_db_model.MessagesCount;
import com.twitch.bot.dynamo_db_model.TwitchAnalysis;
import com.twitch.bot.model.Channel;
import com.twitch.bot.twitch_connection.ChannelsData;

@Lazy(false)
@Component
public class ScheduleTwitchLogic {
    private static final Logger LOG = Logger.getLogger(ScheduleTwitchLogic.class.getName());
    private static final long frequencySeconds = 15000l;
    private Long coolDownMillis = 0l;
    private Long offsetMillis= 0l;
    private TwitchData twitchData;
    private ApiHandler apiHandler;
    private String awsTranscribeBucketName = "TranscribeBucket";

    public ScheduleTwitchLogic(TwitchData twitchData, ApiHandler apiHandler, @Value("${twitch.analysis.cooldown.seconds}") Long coolDownSeconds, @Value("${twitch.analysis.start.offset.minutes}") Long offsetMinutes){
        this.twitchData = twitchData;
        this.apiHandler = apiHandler;
        this.coolDownMillis = coolDownSeconds * 1000;
        this.offsetMillis = offsetMinutes * 60 * 1000;
    }
    @Scheduled(fixedRate = 15000)
    public void jobRunner() throws Exception {
        Long currentTime = System.currentTimeMillis();
        LOG.log(Level.INFO, "currentTime In Schedule ::: " + currentTime);

        JSONObject credentials = twitchData.getCloudCredentials();  
        System.setProperty("aws.accessKeyId", credentials.get("access_id").toString());
        System.setProperty("aws.secretAccessKey", credentials.get("access_key").toString());

        String channelTiming = "";

        List<Channel> allChannelNames = getChannelNames();
        Iterator<Channel> allChannelNamesIter = allChannelNames.iterator();
        while(allChannelNamesIter.hasNext()){
            Channel channel = allChannelNamesIter.next();
            LOG.log(Level.INFO, "Channel Name - {0}", new Object[]{channel.getChannelName()});
            Long startTime = System.currentTimeMillis();
            if(channel.getIsListeningToChannel()){
                processChannelMessages(channel, currentTime);
            }else{
                twitchData.deleteTwitchMessageForChannel(channel, currentTime);
            }
            channelTiming += (channelTiming.trim() == "") ? channel.getChannelName() + " - " + (System.currentTimeMillis() - startTime) : ", " + channel.getChannelName() + " - " + (System.currentTimeMillis() - startTime);
        }
        LOG.log(Level.INFO, "Scheduler Run Time ::: " + channelTiming);
    }

    public List<Channel> getChannelNames() {
        HashMap<String, Channel> data = ChannelsData.getChannels();
        if (null == data || data.size() == 0) {
            return twitchData.getChannelDetails();
        }else{
            return new ArrayList<>(data.values());
        }
    }

    public void processChannelMessages(Channel channel, Long tillTimeStamp) throws Exception {
        JSONObject channelDtls = getChannelDetails(channel);
        if (channelDtls.getBoolean("is_channel_live")) {

            Long startedAt = Long.valueOf(channelDtls.get("stream_started_at").toString());
            JSONArray messages = twitchData.getTwitchMessageForChannel(channel, tillTimeStamp - frequencySeconds,
                    tillTimeStamp);
            twitchData.addMessageCountBasedOnRollingWindow(channel, Long.valueOf(messages.length()), tillTimeStamp);
            Long thresholdValue = getThresholdValueBasedOnChannel(channel);
            if(thresholdValue == -1){
                LOG.log(Level.INFO, "Rolling Window Data not populated for channel {0}", new Object[]{channel.getChannelName()});
            }
            else if(thresholdValue == 0){
                LOG.log(Level.INFO, "No Messages for channel {0}", new Object[]{channel.getChannelName()});
            }
            else if ((startedAt + offsetMillis) >= tillTimeStamp) {
                LOG.log(Level.INFO, "Channel {0} Start Time {1} is under offsetValue {2} for timestamp {3}",
                        new Object[] { channel.getChannelName(), startedAt, offsetMillis, tillTimeStamp });
            }else if (messages.length() >= thresholdValue) {
                LOG.log(Level.INFO,"threshold exceeded for ::: " + channel.getChannelName());
                LOG.log(Level.INFO,"MessageLength for ::: " + messages.length());
                LOG.log(Level.INFO,"thresholdValue ::: " + thresholdValue);
                List<TwitchAnalysis> twitchAnalysis = twitchData.getTwitchAnalysisRawDataOfAChannel(channel,
                        false);
                        LOG.log(Level.INFO,"twitchAnalysis ::: " + twitchAnalysis);
                if (!twitchAnalysis.isEmpty()
                        && (twitchAnalysis.get(0).getTimestamp() + coolDownMillis) > tillTimeStamp) {
                    LOG.log(Level.INFO,
                            "Last Generated Data Time is {0} which is not exceeds the current cooldown of {1} seconds from current time {2}",
                            new Object[] { twitchAnalysis.get(0).getTimestamp(), coolDownMillis, tillTimeStamp });
                    twitchData.deleteTwitchMessageForChannel(channel, tillTimeStamp);
                    return;
                }
                String sentimental_result = awsSentimentalAnalysis(messageMerge(messages));
                LOG.log(Level.INFO,"sentimental_result ::: " + sentimental_result);
                JSONObject clips = awsClipsGeneration(channel);
                LOG.log(Level.INFO,"clips ::: " + clips);
                //awsTranscribeConversion(clips.get("video_url").toString(), channel);
                twitchData.addTwitchAnalysis(channel, sentimental_result, clips, System.currentTimeMillis());
            }
            twitchData.deleteTwitchMessageForChannel(channel, tillTimeStamp);
        } else {
            twitchData.clearMessagesCountForAChannel(channel);
        }
    }

    public JSONObject getChannelDetails(Channel channel) throws Exception{
        String response = apiHandler.setPath(PATH.GET_STREAMS).setParams(new JSONObject().put("user_login", channel.getChannelName())).setHeaders(new JSONObject().put("set_client_id", "Client-Id")).GET();
        JSONObject responseData = new JSONObject(response);
        JSONObject channelDtls = new JSONObject();
        if(responseData.isEmpty()){
            channelDtls.put("is_channel_live", false);
        }else if(responseData.getJSONArray("data").isEmpty()){
            channelDtls.put("is_channel_live", false);
        }else{
            JSONObject data = responseData.getJSONArray("data").getJSONObject(0);
            Boolean isChannelLive = data.get("type").toString().equalsIgnoreCase("live");
            channelDtls.put("is_channel_live", isChannelLive);
            if(isChannelLive){
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                inputFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date date = inputFormat.parse(data.get("started_at").toString());
                channelDtls.put("stream_started_at", date.getTime());
            }   
        }
        return channelDtls;
    }

    public Long getThresholdValueBasedOnChannel(Channel channel) throws Exception {
        if(TwitchData.isAwsEnvironment() && channel.getChannelName().equals("shroud")){
            return 1l;
        }
        List<MessagesCount> msgCountData = twitchData.getMessageCountDataOfAChannel(channel);
        Long thresholdValue = -1l;
        if (!msgCountData.isEmpty()) {
            thresholdValue = 0l;
            for (MessagesCount msgData : msgCountData) {
                thresholdValue += msgData.getMessageCount();
            }
            thresholdValue = thresholdValue / (msgCountData.size() * 4);
        }
        return thresholdValue;
    }

    public String awsSentimentalAnalysis(String messageText){
       
        Region region = Region.US_EAST_1;
       
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

    public void awsTranscribeConversion(String videoUrl, Channel channel){
        if(!isAwsTranscribeBucketExists()){
            createAwsTranscribeBucketExists();
        }
        Region region = Region.US_EAST_1;
        S3Client s3Client = S3Client.builder().region(region)
        .credentialsProvider(SystemPropertyCredentialsProvider.create()).build();

        String key = channel.getTwitchId() + "_" + channel.getId();

        URL url = null;
        try{
            url = new URL(videoUrl);
        }catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception ::: " + ex);
        }


        s3Client.putObject(PutObjectRequest.builder().bucket(awsTranscribeBucketName).key(key)
                .build(), RequestBody.fromByteBuffer(downloadUrl(url)));

    }

    private ByteBuffer downloadUrl(URL toDownload) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    
        try {
            byte[] chunk = new byte[4096];
            int bytesRead;
            InputStream stream = toDownload.openStream();
    
            while ((bytesRead = stream.read(chunk)) > 0) {
                outputStream.write(chunk, 0, bytesRead);
            }
    
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "Exception ::: " + ex);
            return null;
        }
    
        return ByteBuffer.wrap(outputStream.toByteArray());
    }

    public void deleteAwsTranscribeBucketExists() {
        Region region = Region.US_EAST_1;
        S3Client s3Client = S3Client.builder().region(region)
        .credentialsProvider(SystemPropertyCredentialsProvider.create()).build();

        DeleteBucketRequest deleteBucketRequest = DeleteBucketRequest.builder().bucket(awsTranscribeBucketName).build();

        s3Client.deleteBucket(deleteBucketRequest);
    }

    public void createAwsTranscribeBucketExists() {
        Region region = Region.US_EAST_1;
        S3Client s3Client = S3Client.builder().region(region)
        .credentialsProvider(SystemPropertyCredentialsProvider.create()).build();

        CreateBucketRequest createBucketRequest = CreateBucketRequest
            .builder()
            .bucket(awsTranscribeBucketName)
            .createBucketConfiguration(CreateBucketConfiguration.builder()
                .locationConstraint(region.id())
                .build())
            .build();
        
            s3Client.createBucket(createBucketRequest);
    }

    public Boolean isAwsTranscribeBucketExists() {
        Region region = Region.US_EAST_1;
        S3Client s3Client = S3Client.builder().region(region)
                .credentialsProvider(SystemPropertyCredentialsProvider.create()).build();

        HeadBucketRequest request = HeadBucketRequest.builder()
                .bucket(awsTranscribeBucketName)
                .build();

        try {
            s3Client.headBucket(request);
            return true;
        } catch (Exception ex) {
            return false;
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
        LOG.log(Level.INFO,"CLIPS:::responseData in clips 1 ::: " + responseData);
        String clip_id = responseData.getJSONArray("data").getJSONObject(0).getString("id");
        LOG.log(Level.INFO,"CLIPS:::clip_id in clips 1.1 ::: " + clip_id);
        Thread.sleep(5000);//*Thread Sleeps so that the create clip is done generating on twitch side */
        response = apiHandler.setPath(PATH.CLIPS).setParams(new JSONObject().put("id", clip_id)).setHeaders(new JSONObject().put("set_client_id", "Client-Id")).GET();
        responseData = new JSONObject(response);
        LOG.log(Level.INFO,"CLIPS:::responseData in clips 2 ::: " + responseData);
        responseData = responseData.getJSONArray("data").getJSONObject(0);
        data.put("clip_id", clip_id);
        data.put("video_url", responseData.get("url").toString());
        data.put("embed_url", responseData.get("embed_url").toString());
        data.put("created_at", responseData.get("created_at").toString());
        data.put("thumbnail_url", responseData.get("thumbnail_url").toString()); 
        LOG.log(Level.INFO,"CLIPS:::data in clips 3 ::: " + data);
        return data;
    }
}
