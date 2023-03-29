import React, { useEffect, useState } from "react";
import { getTwitchAnalysisOfChannel } from "../services/api-client";
import {
  Card,
  CardHeader,
  CardBody,
  Stack,
  Box,
  Heading,
  StackDivider,
  Button,
  HStack,
  AspectRatio,
} from "@chakra-ui/react";
import { AiFillPlaySquare } from "react-icons/ai";
import {useParams} from "react-router-dom";

interface Clip {
  embed_url: string;
}

interface Analysis {
  clip_details: Clip;
  sentimental_analysis: string;
}

interface Clips {
  channel_name: string;
  twitch_analysis: Analysis[];
}

const Clips = () => {
  const [clips, setClips] = useState<Analysis[]>([]);
  const [error, setError] = useState("");
  const [isEmpty, setEmpty] = useState(true);
  const [showVideo, setVideo] = useState(false);
  const {channel_name} = useParams();

  useEffect(() => {
    debugger;
    getTwitchAnalysisOfChannel(channel_name)
      .then((res) => {
        setClips(res.twitch_analysis);
        if(res.twitch_analysis != undefined && res.twitch_analysis.length != 0){
          setEmpty(false);
        }   
      })
      .catch((err) => setError(err.message));
  }, []);

  if (!isEmpty)
    return (
      <Card>
        <CardHeader>
          <Heading size="md">Generated Clips</Heading>
        </CardHeader>
        <CardBody>
          <Stack divider={<StackDivider />} spacing="4">
            {clips.map((clip, index) => (
              <Box
                key={index}
                border="1px"
                borderColor="whiteAlpha.500"
                borderRadius={3}
                justifyContent="space-between"
                p="4"
              >
                <HStack justifyContent={"space-between"}>
                  <h4>clip {index + 1}</h4>
                  <Button
                    variant="link"
                    float={"right"}
                    onClick={() => setVideo(!showVideo)}
                  >
                    <AiFillPlaySquare size={40} />
                  </Button>
                </HStack>
                {showVideo && (
                  <AspectRatio>
                    <iframe src={clip.clip_details.embed_url + "&parent=localhost"}></iframe>
                  </AspectRatio>
                )}
              </Box>
            ))}
          </Stack>
        </CardBody>
      </Card>
    );
  return <Heading size={"2xl"}>No clips generated</Heading>;
};

export default Clips;
