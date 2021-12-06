import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'

// TS / JS DEPENDENCIES:
import 'bootstrap';
// import 'bootstrap/dist/js/bootstrap.bundle.min'

import '@fortawesome/fontawesome-free/css/all.css'
import '@fortawesome/fontawesome-free/js/all.js'

createApp(App).use(store).use(router).mount('#app')


import '@/assets/js/global.js' // AVOID using JavaScript (just use TypeScript)