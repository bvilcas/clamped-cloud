<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchWithAuth } from '@/utils/fetchWithAuth'

const message = ref('')
const submitting = ref(false)
const submitted = ref(false)
const error = ref('')

// Pre-fill sender info from the authenticated user's profile
const senderName = ref('')
const senderEmail = ref('')

onMounted(async () => {
  try {
    const res = await fetchWithAuth('/api/v1/users/profile')
    if (res.ok) {
      const d = await res.json()
      senderName.value = `${d.data?.firstname ?? ''} ${d.data?.lastname ?? ''}`.trim()
      senderEmail.value = d.data?.email ?? ''
    }
  } catch { /* silent */ }
})

async function handleSubmit() {
  if (!message.value.trim()) return
  submitting.value = true
  error.value = ''
  try {
    const res = await fetchWithAuth('/api/v1/contact', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: message.value.trim() }),
    })
    if (res.ok) {
      submitted.value = true
      message.value = ''
    } else {
      const d = await res.json().catch(() => ({}))
      error.value = d.message ?? 'Something went wrong. Please try again.'
    }
  } catch {
    error.value = 'Could not connect to the server. Please try again later.'
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <v-container class="pa-8" style="max-width: 680px">
    <h1 class="text-info mb-2">Contact Us</h1>
    <p class="intro mb-6">
      Have questions, feedback, or need support with Clamped?
      We'd love to hear from you.
    </p>

    <v-alert v-if="submitted" type="success" variant="tonal" class="mb-4">
      Your message has been sent. We'll get back to you soon!
    </v-alert>

    <v-alert v-if="error" type="error" variant="tonal" class="mb-4">
      {{ error }}
    </v-alert>

    <v-form v-if="!submitted" @submit.prevent="handleSubmit">
      <!-- Read-only sender info (from account) -->
      <v-text-field
        :model-value="senderName"
        label="Your name"
        variant="outlined"
        density="comfortable"
        readonly
        class="mb-2"
      />
      <v-text-field
        :model-value="senderEmail"
        label="Your email"
        type="email"
        variant="outlined"
        density="comfortable"
        readonly
        class="mb-2"
      />
      <v-textarea
        v-model="message"
        label="Message"
        rows="6"
        variant="outlined"
        density="comfortable"
        counter="2000"
        :rules="[v => v.length <= 2000 || 'Max 2000 characters']"
        class="mb-3"
        required
      />
      <v-btn
        type="submit"
        color="info"
        :loading="submitting"
        :disabled="!message.trim()"
      >
        Send Message
      </v-btn>
    </v-form>
  </v-container>
</template>

<style scoped>
.intro {
  color: rgb(var(--v-theme-secondary));
}
</style>
