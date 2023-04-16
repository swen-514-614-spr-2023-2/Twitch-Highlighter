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

interface Props {
  channel: Channel;
}
// { channel }: Props (goes inside())
const ChannelCard = (channelData: any) => {
  const [isSubscribed, setSubscribe] = useState(false);
  const subscribe = () => {
    setSubscribe(!isSubscribed);
  };

  return (
    <Center>
      <Link as={RouteLink} to={channelData.channel.channel_name + "/clips"}>
        <Card borderRadius={10} overflow="hidden" maxW="sm" width="300px">
          {/* <Image src={channel.imageUrl} /> */}
          <Image src={channelImage} />
          <CardBody>
            <HStack justifyContent={"space-between"}>
              <Heading fontSize="2xl">
                {channelData.channel.channel_name}
              </Heading>
              {isSubscribed && (
                <AiFillHeart color="#ff6b81" size={20} onClick={subscribe} />
              )}
              {!isSubscribed && (
                <AiOutlineHeart size={20} onClick={subscribe} />
              )}
            </HStack>
            {/* <Heading fontSize="2xl">Tubbo</Heading> */}
          </CardBody>
        </Card>
      </Link>
    </Center>
  );
};

export default ChannelCard;
