<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router'
import { computed, ref, watch } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
import { useSidebar } from '@/composables/useSidebar'
import Sidebar from '@/components/SideBar.vue'
import TopBar from '@/components/TopBar.vue'
import type { UserDTO } from '@/dto/user-dto'

const route = useRoute()
const authStore = useAuthStore()
const user = ref<UserDTO | null>(null)
const { sidebarVisible } = useSidebar()

const showLayout = computed(() => route.meta.requiresAuth === true)

const fetchProfile = async () => {
  try {
    const res = await fetchWithAuth('/api/v1/users/profile')
    if (res.ok) {
      const data = await res.json()
      user.value = data.data
    }
  } catch (err) {
    console.error('Failed to load profile:', err)
  }
}

watch(
  () => authStore.isAuthenticated,
  (isAuth) => {
    if (isAuth) {
      fetchProfile()
    } else {
      user.value = null
    }
  },
  { immediate: true }
)
</script>

<template>
  <v-app>
    <div v-if="showLayout" class="app-layout">
      <Sidebar v-show="sidebarVisible" />
      <div class="app-main">
        <TopBar :user="user" />
        <main class="app-content">
          <RouterView />
        </main>
      </div>
    </div>
    <div v-else class="full-page">
      <RouterView />
    </div>
  </v-app>
</template>

<style scoped>
.app-layout {
  display: flex;
  height: 100vh;
  background: rgb(var(--v-theme-surface-light));
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-content {
  flex: 1;
  overflow-y: auto;
}

.full-page {
  min-height: 100vh;
  width: 100%;
}
</style>
