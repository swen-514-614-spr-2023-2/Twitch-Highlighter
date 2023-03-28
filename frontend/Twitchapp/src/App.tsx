import { Button, ButtonGroup, Grid, GridItem } from "@chakra-ui/react";
import ChannelGrid from "./components/ChannelGrid";
import NavBar from "./components/NavBar";

function App() {
  return (
    <Grid templateAreas={`"nav" "main"`}>
      <GridItem area="nav">
        <NavBar></NavBar>
      </GridItem>
      <GridItem area="main">
        <ChannelGrid />
      </GridItem>
    </Grid>
  );
}

export default App;
