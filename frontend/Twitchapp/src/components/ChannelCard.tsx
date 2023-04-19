import {
  Card,
  CardBody,
  Center,
  Heading,
  HStack,
  Image,
  Link,
} from "@chakra-ui/react";
import React, { useState } from "react";
import { Channel } from "./ChannelGrid";
import channelImage from "../assets/tubbo.png";
import { Link as RouteLink } from "react-router-dom";
import { AiFillHeart, AiOutlineHeart } from "react-icons/ai";
import { subscribeChannel, unSubscribeChannel } from "../services/api-client";

interface Props {
  channel: Channel;
}
// { channel }: Props (goes inside())
const ChannelCard = (channelData: any) => {
  const [isSubscribed, setSubscribe] = useState(channelData.channel.is_user_subscribed);
  const subscribe = () => {
    if(!isSubscribed){
      subscribeChannel(channelData.channel.id).then(response => {
        debugger;
        if(response.hasOwnProperty("isError") && response.isError){

        }else{
          setSubscribe(!isSubscribed);
        }
      })
    }else{
      unSubscribeChannel(channelData.channel.id).then(response => {
        debugger;
        if(response.hasOwnProperty("isError") && response.isError){

        }else{
          setSubscribe(!isSubscribed);
        }
      })
    }

  };

  
  return (
    <Center>
      <Card borderRadius={10} overflow="hidden" maxW="sm" width="300px">
        {/* <Image src={channel.imageUrl} /> */}
        <Link as={RouteLink} to={channelData.channel.channel_name + "/clips"}>
          <Image src={channelImage} />
        </Link>
        <CardBody>
          <HStack justifyContent={"space-between"}>
            <Link
              as={RouteLink}
              to={channelData.channel.channel_name + "/clips"}
            >
              <Heading fontSize="2xl">
                {channelData.channel.channel_name}
              </Heading>
            </Link>
            {isSubscribed && (
              <AiFillHeart color="#ff6b81" size={20} onClick={subscribe} />
            )}
            {!isSubscribed && <AiOutlineHeart size={20} onClick={subscribe} />}
          </HStack>
        </CardBody>
      </Card>
    </Center>
  );
};

export default ChannelCard;
