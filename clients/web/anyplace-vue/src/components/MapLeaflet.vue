<template>
  <div id="map"></div>
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';

import "leaflet/dist/leaflet.css";
import L from "leaflet";

@Options({
  props: {
    msg: String
  },
  data() {
    return {
      map: null
    };
  },
  mounted() {
    this.bindLeafletOSM(); // WAY1
    // this.bindLeafletWithMapBox();  // WAY2
  },
    beforeDestroy() {
      if (this.map) {
        this.map.remove();
      }
    },
  methods: {
    bindLeafletOSM: function() {
      this.map = L.map('map').setView([51.959, -8.623], 12);
      L.tileLayer("http://{s}.tile.osm.org/{z}/{x}/{y}.png", {
        attribution:
            '&copy; <a href="http://osm.org/copyright">OpenStreetMap</a> contributors'
      }).addTo(this.map);
    },
    bindLeafletWithMapBox: function() {
      const MAPBOX_TOKEN = process.env.VUE_APP_MAPBOX_TOKEN; // sign up: 'https://account.mapbox.com/access-tokens'
      this.map = L.map('map').setView([51.505, -0.09], 13);
      L.tileLayer('https://api.mapbox.com/styles/v1/{id}/tiles/{z}/{x}/{y}?access_token={accessToken}', {
        attribution: 'Map data &copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors, Imagery © <a href="https://www.mapbox.com/">Mapbox</a>',
        maxZoom: 18,
        id: 'mapbox/streets-v11',
        tileSize: 512,
        zoomOffset: -1,
        accessToken: MAPBOX_TOKEN
      }).addTo(this.map);
    }
  }
})

export default class MapLeaflet extends Vue {
  msg!: string /** parameter accepted by the {@link MapLeaflet} class */
}
</script>

<!-- scoped: limits CSS to this component only -->
<style scoped lang="scss">
#map {
    width: 100vw;
    height: 100vh;
}
</style>
