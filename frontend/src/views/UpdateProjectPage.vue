<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
const router = useRouter()
const route = useRoute()
const projectId = route.params.projectId as string

const form = reactive({
  name: '',
  description: ''
})

const loading = ref(true)
const success = ref(false)
const error = ref<string | null>(null)

onMounted(async () => {
  try {
    const res = await fetchWithAuth(
      `/api/v1/userprojects/${projectId}`
    )
    const data = await res.json()

    form.name = data.data.name
    form.description = data.data.description || ''

    loading.value = false
  } catch (err) {
    error.value = 'Failed to load project details.'
  }
})

const handleSubmit = async () => {
  success.value = false
  error.value = null

  try {
    const res = await fetchWithAuth(
      `/api/v1/projects/update/${projectId}`,
      {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name: form.name, description: form.description })
      }
    )

    if (res.ok) {
      success.value = true
    } else {
      error.value = 'Failed to update project.'
    }
  } catch (err) {
    error.value = 'Network or authentication error.'
  }
}
</script>

<template>
  <p v-if="loading">Loading...</p>
  <div v-else class="update-project-container">
    <h1 class="text-info mb-6">Edit Project</h1>

    <v-alert v-if="success" type="success" density="compact" class="mb-4">
      Project updated successfully!
    </v-alert>

    <v-alert v-if="error" type="error" density="compact" class="mb-4">
      {{ error }}
    </v-alert>

    <v-form class="update-card" @submit.prevent="handleSubmit">
      <v-text-field
        v-model="form.name"
        label="Name"
        variant="outlined"
        density="comfortable"
        required
      />

      <v-textarea
        v-model="form.description"
        label="Description"
        variant="outlined"
        density="comfortable"
      />

      <v-btn type="submit" color="info" class="mt-2">
        Save Changes
      </v-btn>

      <v-btn
        v-if="success"
        color="secondary"
        class="mt-2"
        @click="router.push(`/project/${projectId}`)"
      >
        Return to Project
      </v-btn>
    </v-form>
  </div>
</template>

<style scoped>
.update-project-container {
  padding: 2rem;
  max-width: 700px;
  margin: 0 auto;
  font-family: Arial, sans-serif;
  color: rgb(var(--v-theme-on-surface));
}

.update-card {
  background: rgb(var(--v-theme-surface));
  border: 1px solid rgb(var(--v-theme-outline));
  padding: 1.5rem;
  border-radius: 10px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
</style>
