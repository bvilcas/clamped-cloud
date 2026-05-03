<script setup lang="ts">
import { onMounted, ref, reactive } from 'vue'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
import { useAuthStore } from '@/stores/auth'
const authStore = useAuthStore()

const user = ref<{ firstname: string; lastname: string; username: string; role: string } | null>(null)
const updateForm = reactive({ firstName: '', lastName: '' })
const email = ref('')
const passwordForm = reactive({ oldPassword: '', newPassword: '' })

onMounted(async () => {
  try {
    const res = await fetchWithAuth('/api/v1/users/profile')
    if (res.ok) {
      const data = await res.json()
      user.value = data.data
    } else {
      authStore.logout()
    }
  } catch (err) {
    console.error('Failed to load profile:', err)
    authStore.logout()
  }
})

const handleUpdate = async () => {
  try {
    const res = await fetchWithAuth('/api/v1/users/update', {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(updateForm),
    })
    if (res.ok) {
      alert('Name updated!')
      window.location.reload()
    }
  } catch (err) {
    console.error('Update failed:', err)
  }
}

const handleEmailChange = async () => {
  try {
    const res = await fetchWithAuth(`/api/v1/users/update/${email.value}`, {
      method: 'PUT',
    })
    if (res.status === 204) {
      alert('Email changed. Please log in again.')
      authStore.logoutAllSessions()
    }
  } catch (err) {
    console.error('Email change failed:', err)
  }
}

const handlePasswordChange = async () => {
  try {
    const res = await fetchWithAuth('/api/v1/users/change-password', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(passwordForm),
    })
    if (res.status === 204) {
      alert('Password changed. Please log in again.')
      authStore.logoutAllSessions()
    }
  } catch (err) {
    console.error('Password change failed:', err)
  }
}
</script>

<template>
  <v-container class="pa-8">
    <p v-if="!user" class="text-secondary">Loading profile...</p>

    <template v-else>
      <h1 class="text-info mb-6">My Profile</h1>

      <!-- Info -->
      <v-card variant="elevated" elevation="2" class="mb-6">
        <v-card-text>
          <p class="mb-1"><strong>Name:</strong> {{ user.firstname }} {{ user.lastname }}</p>
          <p class="mb-1"><strong>Email:</strong> {{ user.username }}</p>
          <p><strong>Role:</strong> {{ user.role }}</p>
        </v-card-text>
      </v-card>

      <!-- Update Name -->
      <v-card variant="elevated" elevation="2" class="mb-6">
        <v-card-title>Update Name</v-card-title>
        <v-card-text>
          <v-form @submit.prevent="handleUpdate" class="d-flex flex-column ga-2">
            <v-text-field v-model="updateForm.firstName" label="First Name" variant="outlined" density="comfortable" />
            <v-text-field v-model="updateForm.lastName" label="Last Name" variant="outlined" density="comfortable" />
            <div><v-btn type="submit" color="info">Update</v-btn></div>
          </v-form>
        </v-card-text>
      </v-card>

      <!-- Change Email -->
      <v-card variant="elevated" elevation="2" class="mb-6">
        <v-card-title>Change Email</v-card-title>
        <v-card-text>
          <v-form @submit.prevent="handleEmailChange" class="d-flex flex-column ga-2">
            <v-text-field v-model="email" label="New Email" type="email" variant="outlined" density="comfortable" />
            <div><v-btn type="submit" color="info">Change Email</v-btn></div>
          </v-form>
        </v-card-text>
      </v-card>

      <!-- Change Password -->
      <v-card variant="elevated" elevation="2" class="mb-6">
        <v-card-title>Change Password</v-card-title>
        <v-card-text>
          <v-form @submit.prevent="handlePasswordChange" class="d-flex flex-column ga-2">
            <v-text-field v-model="passwordForm.oldPassword" label="Old Password" type="password" variant="outlined" density="comfortable" />
            <v-text-field v-model="passwordForm.newPassword" label="New Password" type="password" variant="outlined" density="comfortable" />
            <div><v-btn type="submit" color="info">Change Password</v-btn></div>
          </v-form>
        </v-card-text>
      </v-card>
    </template>
  </v-container>
</template>
