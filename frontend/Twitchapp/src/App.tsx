import { Button, ButtonGroup, Grid, GridItem } from "@chakra-ui/react";
import {} from "react-router";
import { BrowserRouter as Router, Routes, Route } from "react-router-dom";
import ChannelGrid from "./components/ChannelGrid";
import Clips from "./components/Clips";
import NavBar from "./components/NavBar";

function App() {
  return (
    <Grid templateAreas={`"nav" "main"`}>
      <GridItem area="nav">
        <NavBar></NavBar>
      </GridItem>
      <GridItem area="main">
        <Router>
          <Routes>
            <Route path="/" element={<ChannelGrid />} />
            <Route path="/:channel_name/clips" element={<Clips />} />
          </Routes>
        </Router>
      </GridItem>
    </Grid>
  );
}

export default App;
