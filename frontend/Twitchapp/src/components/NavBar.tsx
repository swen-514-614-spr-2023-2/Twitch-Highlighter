import {
  Button,
  HStack,
  Image,
  Input,
  InputGroup,
  InputLeftElement,
  Link,
} from "@chakra-ui/react";
import React, { useEffect, useRef, useState } from "react";
import logo from "../assets/Twitch_TV.png";
import { AiFillHome } from "react-icons/ai";
import { BsSearch } from "react-icons/bs";
import { Link as RouteLink } from "react-router-dom";

interface Props {
  onSearch: (text: string) => void;
}

const NavBar = ({ onSearch }: Props) => {
  const [searchText, setSearchText] = useState("");
  const ref = useRef<HTMLInputElement>(null);
  useEffect(() => {
    console.log(searchText);
  }, [searchText]);

  const handleSearch = () => {
    const text = ref.current?.value ?? "";
    setSearchText(text);
    onSearch(text);
  };

  return (
    <HStack>
      <Image src={logo} boxSize="55px" />
      {/* <Link as={RouteLink} to={"/subscribedChannels"}>
        <AiFillHome />
      </Link> */}
      <InputGroup>
        <InputLeftElement children={<BsSearch />} />
        <Input
          ref={ref}
          borderRadius={20}
          placeholder="Search twitch channels....."
          variant={"filled"}
          onChange={handleSearch}
        ></Input>
      </InputGroup>
      {/* <Link as={RouteLink} to={"/"}>
        All Channels
      </Link> */}
    </HStack>
  );
};

export default NavBar;
