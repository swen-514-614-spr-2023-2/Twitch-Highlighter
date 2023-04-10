import axios from "axios";

const IS_AWS_BUILD = import.meta.env.VITE_IS_AWS_BUILD;

let domain = "http://localhost:5000";
if(IS_AWS_BUILD){
  domain = import.meta.env.VITE_BACKEND_AWS_URL;
}

export const getChannels = async () => {
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
