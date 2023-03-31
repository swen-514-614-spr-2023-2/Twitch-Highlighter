import axios from "axios";

const IS_AWS_BUILD = process.env.IS_AWS_BUILD;

let domain = "http://localhost:8080";
if(!IS_AWS_BUILD){
  domain = "http://andrewstwitch.us-east-1.elasticbeanstalk.com"; 
}

export const getChannels = async () => {
  console.log(IS_AWS_BUILD);
  console.log(domain);
  const response = await axios.get(domain + "/channels",{
   })
   return response.data;
}


export const getTwitchAnalysisOfChannel = async (channel_name: any) => {
  const response = await axios.get(domain + "/twitch_analysis",{
    params: {
      "channel_name": channel_name
    }
   })
   return response.data;
}
