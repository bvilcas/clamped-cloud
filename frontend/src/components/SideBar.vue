<script setup lang="ts">
import { computed, ref } from "vue"
import { useRoute, useRouter } from "vue-router"
import { useAuthStore } from "@/stores/auth"
import { useSidebar } from "@/composables/useSidebar"

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()
const { toggleSidebar } = useSidebar()

const isProjectsActive = computed(() => route.path.startsWith("/projects") || route.path.startsWith("/project/"))
const isIssuesActive = computed(() => route.path.startsWith("/issues"))
const isEventsActive = computed(() => route.path.startsWith("/events"))
const isMessagesActive = computed(() => route.path.startsWith("/messages"))
const isTeamActive = computed(() => route.path.startsWith("/team"))
const isHelpActive = computed(() => route.path.startsWith("/help"))
const isContactActive = computed(() => route.path.startsWith("/contact"))
const isAboutActive = computed(() => route.path.startsWith("/about"))
const isCalendarActive = computed(() => route.path.startsWith("/calendar"))

const moreOpen = ref(
  route.path.startsWith("/calendar") || route.path.startsWith("/events") // auto-expand if already on a "more" route
)
</script>

<template>
  <aside class="sidebar pa-4">
    <div class="d-flex align-center justify-space-between mb-2" style="margin-left: 17px;">
      <div class="logo" @click="router.push('/dashboard')" style="cursor: pointer;">
        <img src="/logo.png" alt="Clamped Logo" />
      </div>
      <v-btn variant="text" size="small" class="collapse-btn" @click="toggleSidebar">
        «
      </v-btn>
    </div>

    <nav class="nav">
      <v-btn variant="text" block class="nav-item" :class="{ 'nav-item-active': isProjectsActive }"
        prepend-icon="mdi-folder-outline" @click="router.push('/projects')">
        Projects
      </v-btn>
      <v-btn variant="text" block class="nav-item" :class="{ 'nav-item-active': isIssuesActive }"
        prepend-icon="mdi-shield-alert-outline" @click="router.push('/issues')">
        My Issues
      </v-btn>
      <v-btn variant="text" block class="nav-item" :class="{ 'nav-item-active': isMessagesActive }"
        prepend-icon="mdi-message-outline" @click="router.push('/messages')">
        Messages
      </v-btn>
      <v-btn variant="text" block class="nav-item" :class="{ 'nav-item-active': isTeamActive }"
        prepend-icon="mdi-account-group-outline" @click="router.push('/team')">
        Team
      </v-btn>

      <!-- More... collapsible -->
      <v-btn
        variant="text"
        block
        class="nav-item more-btn"
        :prepend-icon="moreOpen ? 'mdi-chevron-up' : 'mdi-chevron-down'"
        @click="moreOpen = !moreOpen"
      >
        More...
      </v-btn>
      <v-expand-transition>
        <div v-show="moreOpen" class="more-section">
          <v-btn
            variant="text"
            block
            class="nav-item nav-item--sub"
            :class="{ 'nav-item-active': isCalendarActive }"
            prepend-icon="mdi-calendar-outline"
            @click="router.push('/calendar')"
          >
            Calendar
          </v-btn>
          <v-btn
            variant="text"
            block
            class="nav-item nav-item--sub"
            :class="{ 'nav-item-active': isEventsActive }"
            prepend-icon="mdi-lightning-bolt-outline"
            @click="router.push('/events')"
          >
            Events
          </v-btn>
        </div>
      </v-expand-transition>
    </nav>

    <div class="d-flex flex-column ga-2 mt-auto mb-4">
      <v-btn variant="text" block class="nav-item" :class="{ 'nav-item-active': isHelpActive }"
        prepend-icon="mdi-help-circle-outline" @click="router.push('/help')">
        Help/Docs
      </v-btn>
      <v-btn variant="text" block class="nav-item" :class="{ 'nav-item-active': isContactActive }"
        prepend-icon="mdi-email-outline" @click="router.push('/contact')">
        Contact Us
      </v-btn>
      <v-btn variant="text" block class="nav-item" :class="{ 'nav-item-active': isAboutActive }"
        prepend-icon="mdi-information-outline" @click="router.push('/about')">
        About Clamped!
      </v-btn>
      <v-btn color="error" block prepend-icon="mdi-logout" @click="authStore.logout()">
        Logout
      </v-btn>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 220px;
  background: rgb(var(--v-theme-surface-variant));
  border-right: 1px solid rgb(var(--v-theme-outline));
  display: flex;
  flex-direction: column;
}

.collapse-btn {
  min-width: unset !important;
  width: 28px !important;
  height: 28px !important;
  font-size: 1rem !important;
  color: rgb(var(--v-theme-secondary)) !important;
  text-transform: none !important;
  letter-spacing: normal !important;
  margin-right: -10px;
}

.collapse-btn :deep(.v-btn__overlay) {
  opacity: 0 !important;
}

.collapse-btn:hover {
  background-color: rgb(var(--v-theme-outline)) !important;
}

.logo {
  width: 100px;
  height: 50px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin: 5px 0;
}

.logo img {
  height: 50px;
  width: 175px;
  display: block;
}

.nav {
  flex: 1;
}

.nav-item {
  justify-content: flex-start !important;
  margin-bottom: 0.3rem;
}

.nav-item-active {
  background-color: rgba(var(--v-theme-on-surface), 0.20) !important;
}

.more-btn {
  color: rgb(var(--v-theme-secondary)) !important;
  font-size: 0.8rem !important;
}

.more-section {
  padding-left: 12px;
}

.nav-item--sub {
  font-size: 0.85rem !important;
}

@media (max-width: 768px) {
  .sidebar {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
    border-right: none;
    border-bottom: 1px solid rgb(var(--v-theme-outline));
  }
}
</style>
