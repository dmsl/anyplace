<template>
  <div id="nav">
    <router-link to="/">Home</router-link> |
    <router-link to="/about">About</router-link>
  </div>
  <router-view/>
</template>

<!-- LOAD GLOBAL TYPESCRIPT AND SCSS -->
<script src="./assets/ts/global.ts"/>
<style lang="scss">@import 'assets/scss/global.scss'; </style>
