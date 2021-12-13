# 1. Vue.js v3 setup:
Instructions on how this project structure was created.
See [SETUP.VUE3](./SETUP.VUE.md)


---

# 2. Build Project

<details closed>

<summary>Instructions to build project</summary>

## Install
```
npm install
```

### Compiles and hot-reloads for development
```
npm run serve
```

### Compiles and minifies for production
```
npm run build
```

### Run your unit tests
```
npm run test:unit
```

### Run your end-to-end tests
```
npm run test:e2e
```

### Lints and fixes files
```
npm run lint
```

### Customize configuration
See [Configuration Reference](https://cli.vuejs.org/config/).

</details>

---

# STRUCTURE:

<details closed>

<summary>Project Structure</summary>


### Vue.js entrypoint:
 - `src/main.ts`: it initializes `src/App.vue`

Global files:

- `src/assets/js/global.js`:
   - Avoid using JavaScript.
   - Use only temporarily for legacy JS code.
   - Included by `main.ts`

- `src/assets/ts/global.ts`:
   - Global TypeScript code
   - Included by `App.vue`


- `src/assets/scss/global.scss`:
   - Global SASS CSS code
   - Included by `App.vue`


</details>

---

# Additional Dependencies Installed

#### Keep the below list up-to-date!

<details closed>

<summary>Dependencies Installed</summary>

## Bootstrap
```bash
npm install bootstrap
```

Customized bootstrap with sass:
- added src/asses/scss/app_bootstrap.scss
- it overrides variables and includes `npm` installed boostrap

## FontAwesome
[@fortawesome](https://www.npmjs.com/package/@fortawesome/fontawesome-free) is the free version of FontAwesome.

```bash
npm install @fortawesome/fontawesome-free
```

## Leaflet
```bash
npm install leaflet
npm install @types/leaflet --save-dev  # for typescript
```

</details>
