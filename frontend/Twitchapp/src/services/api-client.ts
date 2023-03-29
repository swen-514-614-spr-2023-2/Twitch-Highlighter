import axios from "axios";

export const getChannels = async () => {
  const response = await axios.get("http://localhost:8080/channels",{
   })
   return response.data;
}


export const getTwitchAnalysisOfChannel = async (channel_name: any) => {
  const response = await axios.get("http://localhost:8080/twitch_analysis",{
    params: {
      "channel_name": channel_name
    }
   })
   debugger;
   return response.data;
}
