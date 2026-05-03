<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
import { useAuthStore } from '@/stores/auth'
const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  name: '',
  description: ''
})

const error = ref('')
const success = ref('')

const handleSubmit = async () => {
  error.value = ''
  success.value = ''

  if (!form.name.trim()) {
    error.value = 'Project name is required.'
    return
  }

  try {
    console.log('Creating project:', form)

    const res = await fetchWithAuth(
      '/api/v1/projects/create',
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          name: form.name,
          description: form.description
        })
      }
    )

    if (res.ok) {
      const data = await res.json()
      success.value = 'Project created successfully!'

      form.name = ''
      form.description = ''

      setTimeout(() => {
        router.push(`/project/${data.data.id}`)
      }, 900)
    } else {
      const data = await res.json()
      error.value = data.message || 'Failed to create project.'
    }
  } catch (err: unknown) {
    if (err instanceof Error) {
      console.error('Project creation failed:', err.message)
      error.value = 'Network or authentication error.'
      if (err.message === 'Unauthorized after refresh' || err.message === 'Token refresh failed') {
        authStore.logout()
      }
    }
  }
}
</script>

<template>
  <v-form @submit.prevent="handleSubmit" class="create-project-form">
    <h2>Create a New Project</h2>

    <v-text-field
      v-model="form.name"
      label="Project Name *"
      variant="outlined"
      density="comfortable"
      required
    />

    <v-textarea
      v-model="form.description"
      label="Description (Optional)"
      variant="outlined"
      density="comfortable"
      rows="4"
      no-resize
    />

    <v-btn
      type="submit"
      color="info"
      block
      size="large"
      class="mt-2"
    >
      Create Project
    </v-btn>

    <v-alert v-if="error" type="error" density="compact" class="mt-3">
      {{ error }}
    </v-alert>
    <v-alert v-if="success" type="success" density="compact" class="mt-3">
      {{ success }}
    </v-alert>
  </v-form>
</template>

<style scoped>
.create-project-form {
  max-width: 400px;
  margin: 100px auto;
  padding: 30px;
  background-color: rgb(var(--v-theme-surface-variant));
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  font-family: Arial, sans-serif;
}

.create-project-form h2 {
  text-align: center;
  margin-top: 0;
  margin-bottom: 20px;
  color: rgb(var(--v-theme-on-surface));
}
</style>
