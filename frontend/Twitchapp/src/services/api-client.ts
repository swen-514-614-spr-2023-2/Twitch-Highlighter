import axios from "axios";

// const IS_AWS_BUILD = process.env.IS_AWS_BUILD;

const domain = "http://localhost:8080";
// if(!IS_AWS_BUILD){
  
// }

export const getChannels = async () => {
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
