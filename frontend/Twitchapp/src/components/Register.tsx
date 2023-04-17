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
import React, { useState } from "react";
import { Link as RouteLink } from "react-router-dom";

function Register(){
  [email, setEmail] = useState("");
  [username, setUsername] = useState("");
  [password, setPassword] = useState("");

  
  const registerUser = () => {
    debugger;
    console.log(email + "   " + username + "  " + password);
  };

  const setData = (event: any, type: number) => {
    if(type == 0){
      setPassword(event.target.value);
    }else if(type == 1){
      setEmail(event.target.value);
    }else{
      setUsername(event.target.value);
    }
  }

  return (
    <Flex justifyContent={"center"} alignItems="center">
      <Stack justifyContent={"center"} alignItems="center">
        <Box borderColor={"whiteAlpha.600"} borderRadius="20px">
          <form>
            <FormControl>
              <FormLabel htmlFor="email">Email</FormLabel>
              <Input id="email" type={"text"} value={email} onChange={e => setData(e, 1)} placeholder="Email" />
            </FormControl>
            <FormControl>
              <FormLabel htmlFor="username">UserName</FormLabel>
              <Input id="username" type={"text"} value={username} onChange={e => setData(e, 2)} placeholder="Username" />
            </FormControl>
            <FormControl>
              <FormLabel htmlFor="password">Password</FormLabel>
              <Input id="password" type={"password"} value={password} onChange={e => setData(e, 0)} placeholder="*******" />
            </FormControl>
            <Button onClick={registerUser}>Register</Button>
          </form>
        </Box>
      </Stack>
    </Flex>
  );


}
export default Register;
