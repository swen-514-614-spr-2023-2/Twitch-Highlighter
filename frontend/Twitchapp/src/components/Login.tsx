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

const Login = () => {
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
            <Button>Login</Button>
          </form>
        </Box>
        <Box>
          Don't Have An Account?{" "}
          <Link color={"teal"} as={RouteLink} to={"/register"}>
            Register
          </Link>
        </Box>
      </Stack>
    </Flex>
  );
};

export default Login;
