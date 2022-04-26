import Vue from 'vue'
import App from './App.vue'
import router from './router'

Vue.config.productionTip = false

import ElementUI from 'element-ui';
import 'element-ui/lib/theme-chalk/index.css';
import axios from "axios";
import VueAxios from "vue-axios";
import request from "@/utils/request";

Vue.use(ElementUI,{ size: "small" });
Vue.prototype.request = request

new Vue({
  router,
  render: h => h(App)
}).$mount('#app')
