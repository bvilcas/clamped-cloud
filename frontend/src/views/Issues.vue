<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
import { useAuthStore } from '@/stores/auth'
import type { IssueDTO } from '@/dto/issue-dto'

const authStore = useAuthStore()

const reported = ref<IssueDTO[]>([])
const assigned = ref<IssueDTO[]>([])
const verified = ref<IssueDTO[]>([])

onMounted(async () => {
  try {
    const base = '/api/v1/userissues'
    const [rep, asg, ver] = await Promise.all([
      fetchWithAuth(`${base}/reported`),
      fetchWithAuth(`${base}/assigned`),
      fetchWithAuth(`${base}/verified`),
    ])

    if (rep.ok) reported.value = (await rep.json()).data || []
    if (asg.ok) assigned.value = (await asg.json()).data || []
    if (ver.ok) verified.value = (await ver.json()).data || []
  } catch (err) {
    console.error('Failed to fetch issues:', err)
    authStore.logout()
  }
})
</script>

<template>
  <v-container fluid class="pa-8">
    <h1 class="text-info mb-6">My Issues</h1>

    <section class="mb-8">
      <h2 class="section-heading mb-4">Reported by Me</h2>
      <p v-if="reported.length === 0" class="text-secondary">None found.</p>
      <v-row v-else>
        <v-col v-for="v in reported" :key="v.id" cols="12" sm="6" md="4">
          <v-card variant="elevated" elevation="2" class="issue-card h-100">
            <v-card-title>{{ v.title }}</v-card-title>
            <v-card-text>
              <p class="text-caption text-secondary mb-2">{{ v.projectName }}</p>
              <p class="desc text-on-surface-variant mb-2">{{ v.description || 'No description provided.' }}</p>
              <div class="text-secondary text-caption">
                <div v-if="v.type"><strong>Type:</strong> {{ v.type }}</div>
                <div><strong>Severity:</strong> {{ v.severity }}</div>
                <div><strong>Status:</strong> {{ v.status }}</div>
                <div v-if="v.dueAt"><strong>Due:</strong> {{ new Date(v.dueAt).toLocaleDateString() }}</div>
              </div>
              <div
                v-if="v.eventApp || v.eventHost || v.eventType || v.eventExtra"
                class="event-info mt-2 pt-2"
              >
                <div class="event-info-label">Event Info</div>
                <div v-if="v.eventApp"><strong>App:</strong> {{ v.eventApp }}</div>
                <div v-if="v.eventHost"><strong>Host:</strong> {{ v.eventHost }}</div>
                <div v-if="v.eventType"><strong>Type:</strong> {{ v.eventType }}</div>
                <div v-if="v.eventExtra"><strong>Extra:</strong> {{ v.eventExtra }}</div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </section>

    <section class="mb-8">
      <h2 class="section-heading mb-4">Assigned to Me</h2>
      <p v-if="assigned.length === 0" class="text-secondary">None found.</p>
      <v-row v-else>
        <v-col v-for="v in assigned" :key="v.id" cols="12" sm="6" md="4">
          <v-card variant="elevated" elevation="2" class="issue-card h-100">
            <v-card-title>{{ v.title }}</v-card-title>
            <v-card-text>
              <p class="text-caption text-secondary mb-2">{{ v.projectName }}</p>
              <p class="desc text-on-surface-variant mb-2">{{ v.description || 'No description provided.' }}</p>
              <div class="text-secondary text-caption">
                <div v-if="v.type"><strong>Type:</strong> {{ v.type }}</div>
                <div><strong>Severity:</strong> {{ v.severity }}</div>
                <div><strong>Status:</strong> {{ v.status }}</div>
                <div v-if="v.dueAt"><strong>Due:</strong> {{ new Date(v.dueAt).toLocaleDateString() }}</div>
              </div>
              <div
                v-if="v.eventApp || v.eventHost || v.eventType || v.eventExtra"
                class="event-info mt-2 pt-2"
              >
                <div class="event-info-label">Event Info</div>
                <div v-if="v.eventApp"><strong>App:</strong> {{ v.eventApp }}</div>
                <div v-if="v.eventHost"><strong>Host:</strong> {{ v.eventHost }}</div>
                <div v-if="v.eventType"><strong>Type:</strong> {{ v.eventType }}</div>
                <div v-if="v.eventExtra"><strong>Extra:</strong> {{ v.eventExtra }}</div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </section>

    <section class="mb-8">
      <h2 class="section-heading mb-4">Verified by Me</h2>
      <p v-if="verified.length === 0" class="text-secondary">None found.</p>
      <v-row v-else>
        <v-col v-for="v in verified" :key="v.id" cols="12" sm="6" md="4">
          <v-card variant="elevated" elevation="2" class="issue-card h-100">
            <v-card-title>{{ v.title }}</v-card-title>
            <v-card-text>
              <p class="text-caption text-secondary mb-2">{{ v.projectName }}</p>
              <p class="desc text-on-surface-variant mb-2">{{ v.description || 'No description provided.' }}</p>
              <div class="text-secondary text-caption">
                <div v-if="v.type"><strong>Type:</strong> {{ v.type }}</div>
                <div><strong>Severity:</strong> {{ v.severity }}</div>
                <div><strong>Status:</strong> {{ v.status }}</div>
                <div v-if="v.dueAt"><strong>Due:</strong> {{ new Date(v.dueAt).toLocaleDateString() }}</div>
              </div>
              <div
                v-if="v.eventApp || v.eventHost || v.eventType || v.eventExtra"
                class="event-info mt-2 pt-2"
              >
                <div class="event-info-label">Event Info</div>
                <div v-if="v.eventApp"><strong>App:</strong> {{ v.eventApp }}</div>
                <div v-if="v.eventHost"><strong>Host:</strong> {{ v.eventHost }}</div>
                <div v-if="v.eventType"><strong>Type:</strong> {{ v.eventType }}</div>
                <div v-if="v.eventExtra"><strong>Extra:</strong> {{ v.eventExtra }}</div>
              </div>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </section>
  </v-container>
</template>

<style scoped>
.section-heading {
  font-size: 1rem;
  font-weight: 600;
  color: rgb(var(--v-theme-on-surface));
  letter-spacing: 0;
}

.issue-card {
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.issue-card:hover {
  transform: translateY(-3px);
}

.desc {
  display: -webkit-box;
  -webkit-line-clamp: 3;
  line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.event-info {
  border-top: 1px solid rgba(var(--v-theme-on-surface), 0.08);
  font-size: 0.75rem;
}

.event-info-label {
  font-size: 0.68rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: rgb(var(--v-theme-on-surface-variant));
  margin-bottom: 2px;
}
</style>
