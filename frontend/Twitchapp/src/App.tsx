import { Button, ButtonGroup, Grid, GridItem } from "@chakra-ui/react";
import { useState } from "react";
import {} from "react-router";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import ChannelGrid from "./components/ChannelGrid";
import Clips from "./components/Clips";
import NavBar from "./components/NavBar";
import Register from "./components/Register";
import Login from "./components/Login";

function App() {
  const [searchText, setSearchText] = useState("");
  const handleSearch = (text: string) => {
    setSearchText(text);
  };
  return (
    <Grid templateAreas={`"nav" "main"`}>
      <GridItem area="nav">
        <NavBar onSearch={handleSearch}></NavBar>
      </GridItem>
      <GridItem area="main">
        <Router>
          <Routes>
            {localStorage.getItem("isLogIn") && <Route path="/" element={<ChannelGrid searchText={searchText} />} />}
            {!localStorage.getItem("isLogIn") && <Route path="/" element={<Login />} />}
            <Route path="/:channel_name/clips" element={<Clips />} />
            <Route path="/register" element={<Register />} />
          </Routes>
        </Router>
      </GridItem>
    </Grid>
  );
}

export default App;
