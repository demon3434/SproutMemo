import { createApp } from 'vue';
import App from './App.vue';
import router from './router.js';

// Import stationery fonts synced from design-system/fonts
import './assets/fonts/nunito/500.css';
import './assets/fonts/nunito/600.css';
import './assets/fonts/nunito/700.css';
import './assets/fonts/nunito/800.css';
import './assets/fonts/lxgw-wenkai-lite/lxgwwenkailite-regular.css';
import './assets/fonts/lxgw-wenkai-lite/lxgwwenkailite-bold.css';

// Import CSS variables and styles globally
import './style.css';

const app = createApp(App);
app.use(router);
app.mount('#app');
