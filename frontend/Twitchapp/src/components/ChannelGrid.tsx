import { Center, list, SimpleGrid } from "@chakra-ui/react";
import React, { useEffect, useState } from "react";
import { getChannels } from "../services/api-client";
import ChannelCard from "./ChannelCard";

export interface Channel {
  id: number;
  imageUrl: string;
  name: string;
}

const ChannelGrid = () => {
  const [channels, setchannel] = useState<Channel[]>([]);
  const [error, setError] = useState("");

    useEffect(() => {
      getChannels()
        .then(
          (res) => {
            setchannel(res)
          })
        .catch((err) => setError(err.message));
    }, []);

  return (
    <Center>
      <SimpleGrid columns={3} spacing={10} padding="10px">
        {channels.map((channel) => (
        <ChannelCard key={channel.id} channel={channel} />
      ))}
        {/* <ChannelCard key="1" /> */}
      </SimpleGrid>
    </Center>
  );
};

export default ChannelGrid;
