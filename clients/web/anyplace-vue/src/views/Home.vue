<template>
  <div class="home">
    <MapLeaflet msg="Input Text to LeafletMap"/>
  </div>
</template>

<script lang="ts">
import { Options, Vue } from 'vue-class-component';
import MapLeaflet from "@/components/MapLeaflet.vue"; // @ is an alias to /src

@Options({
  components: {
    MapLeaflet,
  },
})
export default class Home extends Vue {}
</script>
