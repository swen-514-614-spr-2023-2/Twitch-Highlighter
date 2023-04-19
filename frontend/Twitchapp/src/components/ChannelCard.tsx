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
import tubbo from "../assets/tubbo.png";
import summit1g from "../assets/summit1g.png";
import shroud from "../assets/shroud.png";
import xQc from "../assets/xQc.png";
import { Link as RouteLink } from "react-router-dom";
import { AiFillHeart, AiOutlineHeart } from "react-icons/ai";

const ChannelCard = (channelData: any) => {
  const [isSubscribed, setSubscribe] = useState(false);
  const subscribe = () => {
    setSubscribe(!isSubscribed);
  };
  return (
    <Center>
      <Card borderRadius={10} overflow="hidden" maxW="sm" width="300px">
        <Link as={RouteLink} to={channelData.channel.channel_name + "/clips"}>
          <Image src={channelData.channel.channel_name.toLowerCase()} />
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
