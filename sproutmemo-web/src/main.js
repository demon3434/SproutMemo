import { createApp } from 'vue';
import App from './App.vue';
import router from './router.js';

// Import CSS variables and styles globally
import './style.css';

const app = createApp(App);
app.use(router);
app.mount('#app');
