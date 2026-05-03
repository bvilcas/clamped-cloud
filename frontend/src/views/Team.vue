<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

interface Project {
  id: string
  name: string
  description: string
  myRole: string
}

interface Member {
  id: string
  firstname: string
  lastname: string
  projectRole: string
}

const projects = ref<Project[]>([])
const membersByProject = ref<Record<string, Member[]>>({})
const loading = ref(true)

const roleColor = (role: string) => {
  if (role === 'LEAD') return 'warning'
  if (role === 'PROGRAMMER') return 'info'
  return 'secondary'
}

onMounted(async () => {
  try {
    const res = await fetchWithAuth('/api/v1/userprojects/all')
    if (!res.ok) { authStore.logout(); return }
    const data = await res.json()
    projects.value = data.data || []

    await Promise.all(projects.value.map(async (p) => {
      const mRes = await fetchWithAuth(`/api/v1/userprojects/members/${p.id}`)
      if (mRes.ok) {
        const mData = await mRes.json()
        membersByProject.value[p.id] = mData.data || []
      }
    }))
  } catch {
    authStore.logout()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <v-container class="pa-8">
    <h1 class="text-info mb-1">Team</h1>
    <p class="text-secondary mb-6">Your teammates across all projects you're a part of.</p>

    <p v-if="loading" class="text-secondary">Loading teams...</p>

    <p v-else-if="projects.length === 0" class="text-secondary">
      You're not a member of any projects yet.
    </p>

    <v-row v-else>
      <v-col v-for="p in projects" :key="p.id" cols="12" md="6">
        <v-card variant="elevated" elevation="2" class="h-100">
          <v-card-title class="d-flex justify-space-between align-center">
            <span>{{ p.name }}</span>
            <v-chip :color="roleColor(p.myRole)" size="small" variant="outlined">
              {{ p.myRole }}
            </v-chip>
          </v-card-title>

          <v-card-text>
            <p v-if="p.description" class="text-secondary text-caption mb-3">{{ p.description }}</p>

            <template v-if="!membersByProject[p.id]">
              <p class="text-secondary text-caption">Loading members...</p>
            </template>
            <template v-else-if="membersByProject[p.id].length === 0">
              <p class="text-secondary text-caption">No members found.</p>
            </template>
            <template v-else>
              <template v-for="(m, i) in membersByProject[p.id]" :key="m.id">
                <v-divider v-if="i > 0" class="my-1" />
                <div class="d-flex align-center justify-space-between py-1">
                  <div class="d-flex align-center ga-2">
                    <v-avatar size="28" color="surface-variant">
                      <span class="text-caption font-weight-bold">
                        {{ m.firstname[0] }}{{ m.lastname[0] }}
                      </span>
                    </v-avatar>
                    <span class="text-body-2">{{ m.firstname }} {{ m.lastname }}</span>
                  </div>
                  <v-chip :color="roleColor(m.projectRole)" size="x-small" variant="tonal">
                    {{ m.projectRole }}
                  </v-chip>
                </div>
              </template>
            </template>

            <div class="mt-4">
              <v-btn
                size="small"
                color="info"
                variant="outlined"
                @click="router.push(`/project/${p.id}`)"
              >
                Go to Project
              </v-btn>
            </div>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
