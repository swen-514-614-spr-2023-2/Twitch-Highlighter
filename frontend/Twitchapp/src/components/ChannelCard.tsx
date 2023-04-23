import {
  Card,
  CardBody,
  Center,
  Heading,
  HStack,
  Image,
  Link,
} from "@chakra-ui/react";
import React, { useState, useRef } from "react";
import { Channel } from "./ChannelGrid";
import tubbo from "../assets/tubbo.png";
import summit1g from "../assets/summit1g.png";
import shroud from "../assets/shroud.png";
import xQc from "../assets/xQc.png";
import { Link as RouteLink } from "react-router-dom";
import { AiFillHeart, AiOutlineHeart } from "react-icons/ai";
import { subscribeChannel, unSubscribeChannel } from "../services/api-client";

const ChannelCard = (channelData: any) => {
  const subRef = useRef(null);
  const [isSubscribed, setSubscribe] = useState(
    channelData.channel.is_user_subscribed
  );
  const [isSubscribeAPIHit, setIsSubscribeAPIHit] = useState(false);
  const subscribe = () => {
    if (!isSubscribed) {
      subscribeChannel(channelData.channel.id).then((response) => {
        if (response.hasOwnProperty("isError") && response.isError) {
        } else {
          setSubscribe(!isSubscribed);
        }
      });
    } else {
      unSubscribeChannel(channelData.channel.id).then((response) => {
        if (response.hasOwnProperty("isError") && response.isError) {
        } else {
          setSubscribe(!isSubscribed);
        }
      });
    }
  };
  return (
    <Center>
      <Card borderRadius={10} overflow="hidden" maxW="sm" width="300px">
        <Link as={RouteLink} to={channelData.channel.channel_name + "/clips"}>
          <Image src={tubbo} />
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
              <button onClick={subscribe} disabled={isSubscribeAPIHit}>
                <AiFillHeart color="#ff6b81" size={20} />
              </button>
            )}
            {!isSubscribed && (
              <button onClick={subscribe} disabled={isSubscribeAPIHit}>
                <AiOutlineHeart size={20} onClick={subscribe} />
              </button>
            )}
          </HStack>
        </CardBody>
      </Card>
    </Center>
  );
};

export default ChannelCard;
