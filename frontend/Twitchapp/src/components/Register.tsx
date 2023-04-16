import {
  Box,
  Button,
  Flex,
  FormControl,
  FormLabel,
  Input,
  Link,
  Stack,
} from "@chakra-ui/react";
import React from "react";
import { Link as RouteLink } from "react-router-dom";
const Register = () => {
  return (
    <Flex justifyContent={"center"} alignItems="center">
      <Stack justifyContent={"center"} alignItems="center">
        <Box borderColor={"whiteAlpha.600"} borderRadius="20px">
          <form>
            <FormControl>
              <FormLabel htmlFor="username">UserName</FormLabel>
              <Input id="username" type={"text"} placeholder="UserName" />
            </FormControl>
            <FormControl>
              <FormLabel htmlFor="password">Password</FormLabel>
              <Input id="password" type={"password"} placeholder="*******" />
            </FormControl>
            <Button>Register</Button>
          </form>
        </Box>
      </Stack>
    </Flex>
  );
};

export default Register;
