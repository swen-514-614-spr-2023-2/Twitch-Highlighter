import { Card, CardBody, Heading, Image } from "@chakra-ui/react";
import React from "react";
import { Channel } from "./ChannelGrid";

interface Props {
  channel: Channel;
}

const ChannelCard = ({ channel }: Props) => {
  return (
    <Card borderRadius={10} overflow="hidden">
      <Image src={channel.imageUrl} />
      <CardBody>
        <Heading fontSize="2xl">{channel.name}</Heading>
      </CardBody>
    </Card>
  );
};

export default ChannelCard;
