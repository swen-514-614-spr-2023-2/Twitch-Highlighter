import React, { useEffect, useState } from "react";
import apiClient from "../services/api-client";
import {
  Card,
  CardHeader,
  CardBody,
  Stack,
  Box,
  Heading,
  StackDivider,
} from "@chakra-ui/react";

const [clips, setClips] = useState([]);
const [error, setError] = useState("");

useEffect(() => {
  apiClient
    .get("/getclips")
    .then((res) => setClips(res.data))
    .catch((err) => setError(err.message));
}, []);

const Clips = () => {
  return (
    <Card>
      <CardHeader>
        <Heading size="md">Generated Clips</Heading>
      </CardHeader>
      <CardBody>
        <Stack divider={<StackDivider />} spacing="4">
          {clips.map((clip, index) => (
            <Box>
              <h4>clip {index + 1}</h4>
              <button></button>
            </Box>
          ))}
        </Stack>
      </CardBody>
    </Card>
  );
};

export default Clips;
