import { Card, CardBody, Center, Heading, Image, Link } from "@chakra-ui/react";
import React from "react";
import { Channel } from "./ChannelGrid";
import channelImage from "../assets/tubbo.png";
import { Link as RouteLink } from "react-router-dom";

interface Props {
  channel: Channel;
}
// { channel }: Props (goes inside())
const ChannelCard = () => {
  return (
    <Center>
      <Link as={RouteLink} to="/clips">
        <Card borderRadius={10} overflow="hidden" maxW="sm">
          {/* <Image src={channel.imageUrl} /> */}
          <Image src={channelImage} />
          <CardBody>
            {/* <Heading fontSize="2xl">{channel.name}</Heading> */}
            <Heading fontSize="2xl">Tubbo</Heading>
          </CardBody>
        </Card>
      </Link>
    </Center>
  );
};

export default ChannelCard;
