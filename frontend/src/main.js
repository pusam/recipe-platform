import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import App from './App.vue'
import { authStore } from './stores/auth'
import { setUnauthorizedHandler } from './utils/api'

import HomePage from './views/HomePage.vue'
import RecipeListPage from './views/RecipeListPage.vue'
import RecipeDetailPage from './views/RecipeDetailPage.vue'
import RecipeEditPage from './views/RecipeEditPage.vue'
import LoginPage from './views/LoginPage.vue'
import SignupPage from './views/SignupPage.vue'
import MyFavoritesPage from './views/MyFavoritesPage.vue'
import ProfilePage from './views/ProfilePage.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'Home', component: HomePage },
    { path: '/recipes', name: 'Recipes', component: RecipeListPage, meta: { requiresAuth: true } },
    { path: '/recipes/:id', name: 'RecipeDetail', component: RecipeDetailPage, props: true },
    {
      path: '/recipes/:id/edit',
      name: 'RecipeEdit',
      component: RecipeEditPage,
      props: true,
      meta: { requiresAuth: true }
    },
    { path: '/login', name: 'Login', component: LoginPage, meta: { standalone: true } },
    { path: '/signup', name: 'Signup', component: SignupPage, meta: { standalone: true } },
    {
      path: '/favorites',
      name: 'Favorites',
      component: MyFavoritesPage,
      meta: { requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'Profile',
      component: ProfilePage,
      meta: { requiresAuth: true }
    }
  ]
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
})

setUnauthorizedHandler(() => {
  const current = router.currentRoute.value
  router.push({
    path: '/login',
    query: {
      redirect: current.fullPath,
      expired: '1'
    }
  })
})

createApp(App).use(router).mount('#app')
