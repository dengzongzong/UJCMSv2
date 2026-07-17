import Vue from 'vue'
import Vuex from 'vuex'
import admin from './modules/admin'
import app from './modules/app'
import getters from './getters'

Vue.use(Vuex)

const store = new Vuex.Store({
  modules: {
    admin,
    app
  },
  getters
})

export default store
