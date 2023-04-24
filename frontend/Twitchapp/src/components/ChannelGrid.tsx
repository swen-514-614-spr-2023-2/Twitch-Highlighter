import {
  Center,
  Heading,
  HStack,
  list,
  SimpleGrid,
  Switch,
  Text,
} from "@chakra-ui/react";
import React, { useEffect, useState } from "react";
import { getChannels } from "../services/api-client";
import ChannelCard from "./ChannelCard";
import ChannelCardSkeleton from "./ChannelCardSkeleton";

export interface Channel {
  twitch_id: number;
  channel_name: string;
  id: number;
  is_user_subscribed: boolean;
}

interface Props {
  searchText: string;
}

const ChannelGrid = ({ searchText }: Props) => {
  const [channels, setchannel] = useState<Channel[]>([]);
  const [error, setError] = useState("");
  const [isLoading, setLoading] = useState(true);
  const [showSubscribed, setSubscribed] = useState(false);
  const skeletons = [1, 2, 3, 4, 5, 6];

  useEffect(() => {
    getChannels()
      .then((res) => {
        setchannel(res);
        setLoading(false);
      })
      .catch((err) => {
        setError(err.message);
        setLoading(false);
      });
  }, [searchText]);

  const filteredChannels = channels.filter((channel) => {
    if (searchText) {
      return (
        channel.channel_name &&
        channel.channel_name.toLowerCase().includes(searchText.toLowerCase())
      );
    } else {
      return true;
    }
  });

  const showChannels = filteredChannels.filter((channel) => {
    if (showSubscribed) {
      return channel.channel_name && channel.is_user_subscribed;
    } else {
      return true;
    }
  });

  if (filteredChannels.length !== 0)
    return (
      <>
        <HStack>
          <Text as="b">Show subscribed channels?</Text>
          <Switch
            isChecked={showSubscribed === true}
            onChange={() => setSubscribed(!showSubscribed)}
          />
        </HStack>
        <Center>
          <SimpleGrid columns={3} spacing={10} padding="10px">
            {showChannels.map((channel) => (
              <ChannelCard key={channel.id} channel={channel} />
            ))}
          </SimpleGrid>
        </Center>
      </>
    );
  else
    return (
      <Center>
        <SimpleGrid columns={3} spacing={10} padding="10px">
          {skeletons.map((skeleton) => (
            <ChannelCardSkeleton key={skeleton} />
          ))}
        </SimpleGrid>
      </Center>
    );
};

export default ChannelGrid;
