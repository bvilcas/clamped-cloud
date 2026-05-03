<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
import { useAuthStore } from '@/stores/auth'
const router = useRouter()
const authStore = useAuthStore()

interface ProjectWithRole {
  id: string
  name: string
  description: string
  myRole: string
  createdAt: string
}

const projects = ref<ProjectWithRole[]>([])

onMounted(async () => {
  try {
    const res = await fetchWithAuth('/api/v1/userprojects/all')

    if (res.ok) {
      const data = await res.json()
      console.log('PROJECT RESPONSE:', data)
      console.log('FIRST PROJECT:', data.data?.[0])
      projects.value = data.data || []
    }
  } catch (err: unknown) {
    if (err instanceof Error) {
      console.error('Failed to load projects:', err.message)
    }
    authStore.logout()
  }
})
</script>

<template>
  <v-container class="pa-8">
    <h1 class="text-info mb-6">My Projects</h1>

    <p v-if="projects.length === 0" class="text-secondary">No projects found.</p>

    <v-row v-else>
      <v-col
        v-for="p in projects"
        :key="p.id"
        cols="12" sm="6" md="4"
      >
        <v-card
          variant="elevated"
          elevation="2"
          class="project-card h-100"
          @click="router.push(`/project/${p.id}`)"
        >
          <v-card-title>{{ p.name }}</v-card-title>
          <v-card-text>
            <p class="desc text-on-surface-variant mb-2">{{ p.description || 'No description provided.' }}</p>
            <div class="text-secondary text-caption">
              <div><strong>Role:</strong> {{ p.myRole }}</div>
              <div v-if="p.createdAt"><strong>Created:</strong> {{ new Date(p.createdAt).toLocaleDateString() }}</div>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<style scoped>
.project-card {
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.project-card:hover {
  transform: translateY(-3px);
}

.desc {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
