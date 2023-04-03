import axios from "axios";

const IS_AWS_BUILD = import.meta.env.IS_AWS_BUILD;

let domain = "http://localhost:8080";
if(!IS_AWS_BUILD){
  domain = "https://qa3hncyg0j.execute-api.us-east-1.amazonaws.com/dev"; 
}

export const getChannels = async () => {
  console.log(IS_AWS_BUILD);
  console.log(import.meta.env);
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
