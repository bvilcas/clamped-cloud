<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { applySettings, loadSettings, saveSettings } from '@/utils/settingsStorage'
import { fetchWithAuth } from '@/utils/fetchWithAuth'

const authStore = useAuthStore()

// ── Email notifications (persisted in DB) ─────────────────────
const emailNotificationsEnabled = ref(false)
const emailNotifLoading = ref(false)
const emailNotifSaved = ref(false)
const accountEmail = ref('')

const fetchEmailNotifSetting = async () => {
  try {
    const [notifRes, profileRes] = await Promise.all([
      fetchWithAuth('/api/v1/users/email-notifications'),
      fetchWithAuth('/api/v1/users/profile'),
    ])
    if (notifRes.ok) {
      const d = await notifRes.json()
      emailNotificationsEnabled.value = d.data ?? false
    }
    if (profileRes.ok) {
      const d = await profileRes.json()
      accountEmail.value = d.data?.email ?? ''
    }
  } catch { /* silent */ }
}

const handleEmailNotifSave = async () => {
  emailNotifLoading.value = true
  emailNotifSaved.value = false
  try {
    const res = await fetchWithAuth(
      `/api/v1/users/email-notifications?enabled=${emailNotificationsEnabled.value}`,
      { method: 'PUT' }
    )
    if (res.ok) {
      emailNotifSaved.value = true
      setTimeout(() => { emailNotifSaved.value = false }, 3000)
    }
  } catch { /* silent */ } finally {
    emailNotifLoading.value = false
  }
}

// ── Display / Preferences (local storage) ─────────────────────
const displayMode = ref('system')
const textSize = ref('medium')
const defaultPage = ref('projects')
const autoOpenLastProject = ref(true)
const displaySaved = ref(false)
const prefSaved = ref(false)

onMounted(async () => {
  const settings = loadSettings()
  displayMode.value = settings.displayMode
  textSize.value = settings.textSize
  defaultPage.value = settings.defaultPage
  autoOpenLastProject.value = settings.autoOpenLastProject
  applySettings(settings)
  await fetchEmailNotifSetting()
})

const handleDisplaySave = () => {
  const next = { ...loadSettings(), displayMode: displayMode.value, textSize: textSize.value }
  saveSettings(next)
  applySettings(next)
  displaySaved.value = true
  setTimeout(() => { displaySaved.value = false }, 3000)
}

const handlePreferencesSave = () => {
  const next = { ...loadSettings(), defaultPage: defaultPage.value, autoOpenLastProject: autoOpenLastProject.value }
  saveSettings(next)
  prefSaved.value = true
  setTimeout(() => { prefSaved.value = false }, 3000)
}

const handleLogoutAllSessions = async () => {
  const confirmed = window.confirm('Log out of all sessions?')
  if (confirmed) {
    await authStore.logoutAllSessions()
  }
}

const displayModeItems = [
  { title: 'System', value: 'system' },
  { title: 'Light', value: 'light' },
  { title: 'Dark', value: 'dark' },
]

const textSizeItems = [
  { title: 'Small text', value: 'small' },
  { title: 'Medium text', value: 'medium' },
  { title: 'Large text', value: 'large' },
]

const defaultPageItems = [
  { title: 'Default to Projects', value: 'projects' },
  { title: 'Default to Reports', value: 'reports' },
  { title: 'Default to Dashboard', value: 'dashboard' },
]
</script>

<template>
  <v-container class="pa-8">
    <h1 class="text-info mb-6">Settings</h1>

    <!-- Email Notifications -->
    <v-card variant="elevated" elevation="2" class="mb-6">
      <v-card-title>Email Notifications</v-card-title>
      <v-card-text>
        <p class="text-body-2 text-secondary mb-3">
          Receive an email at <strong>{{ accountEmail || 'your account email' }}</strong> whenever
          you get an in-app notification (assignments, project changes, new reports, etc.).
        </p>
        <v-switch
          v-model="emailNotificationsEnabled"
          :label="emailNotificationsEnabled ? 'Email notifications on' : 'Email notifications off'"
          color="info"
          hide-details
          density="compact"
          class="mb-3"
        />
        <v-alert
          v-if="emailNotifSaved"
          type="success"
          variant="tonal"
          density="compact"
          class="mb-3"
        >Settings saved.</v-alert>
        <v-btn
          color="info"
          :loading="emailNotifLoading"
          @click="handleEmailNotifSave"
        >Save</v-btn>
      </v-card-text>
    </v-card>

    <!-- Display -->
    <v-card variant="elevated" elevation="2" class="mb-6">
      <v-card-title>Display</v-card-title>
      <v-card-text>
        <v-form @submit.prevent="handleDisplaySave" class="d-flex flex-column ga-2">
          <v-select
            v-model="displayMode"
            :items="displayModeItems"
            label="Theme"
            variant="outlined"
            density="comfortable"
          />
          <v-select
            v-model="textSize"
            :items="textSizeItems"
            label="Text Size"
            variant="outlined"
            density="comfortable"
          />
          <v-alert v-if="displaySaved" type="success" variant="tonal" density="compact" class="mb-1">Settings saved.</v-alert>
          <div><v-btn type="submit" color="info">Save</v-btn></div>
        </v-form>
      </v-card-text>
    </v-card>

    <!-- Preferences -->
    <v-card variant="elevated" elevation="2" class="mb-6">
      <v-card-title>Preferences</v-card-title>
      <v-card-text>
        <v-form @submit.prevent="handlePreferencesSave" class="d-flex flex-column ga-2">
          <v-select
            v-model="defaultPage"
            :items="defaultPageItems"
            label="Default Page"
            variant="outlined"
            density="comfortable"
          />
          <v-checkbox
            v-model="autoOpenLastProject"
            label="Auto-open last project"
            density="compact"
            hide-details
          />
          <v-alert v-if="prefSaved" type="success" variant="tonal" density="compact" class="mb-1">Settings saved.</v-alert>
          <div><v-btn type="submit" color="info">Save</v-btn></div>
        </v-form>
      </v-card-text>
    </v-card>

    <!-- Logout -->
    <v-card variant="elevated" elevation="2" class="mb-6">
      <v-card-title>Logout</v-card-title>
      <v-card-text>
        <v-btn color="error" @click="handleLogoutAllSessions">
          Logout of all sessions
        </v-btn>
      </v-card-text>
    </v-card>
  </v-container>
</template>
