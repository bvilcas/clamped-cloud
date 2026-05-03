<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
import { useAuthStore } from '@/stores/auth'
import type { VulnerabilityDTO } from '@/dto/vulnerability-dto'
const authStore = useAuthStore()

const reported = ref<VulnerabilityDTO[]>([])
const assigned = ref<VulnerabilityDTO[]>([])
const verified = ref<VulnerabilityDTO[]>([])

onMounted(async () => {
  try {
    const base = '/api/v1/uservulns'
    const [rep, asg, ver] = await Promise.all([
      fetchWithAuth(`${base}/reported`),
      fetchWithAuth(`${base}/assigned`),
      fetchWithAuth(`${base}/verified`),
    ])

    if (rep.ok) reported.value = (await rep.json()).data || []
    if (asg.ok) assigned.value = (await asg.json()).data || []
    if (ver.ok) verified.value = (await ver.json()).data || []
  } catch (err) {
    console.error('Failed to fetch vulnerabilities:', err)
    authStore.logout()
  }
})
</script>

<template>
  <v-container class="pa-8">
    <h1 class="text-info mb-6">My Vulnerabilities</h1>

    <section class="mb-8">
      <h2 class="section-heading mb-4">Reported by Me</h2>
      <p v-if="reported.length === 0" class="text-secondary">None found.</p>
      <v-row v-else>
        <v-col v-for="v in reported" :key="v.id" cols="12" sm="6" md="4">
          <v-card variant="elevated" elevation="2" class="vuln-card h-100">
            <v-card-title>{{ v.title }}</v-card-title>
            <v-card-text>
              <p class="text-caption text-secondary mb-2">{{ v.projectName }}</p>
              <p class="desc text-on-surface-variant mb-2">{{ v.description || 'No description provided.' }}</p>
              <div class="text-secondary text-caption">
                <div><strong>Severity:</strong> {{ v.severity }}</div>
                <div><strong>Status:</strong> {{ v.status }}</div>
                <div v-if="v.dueAt"><strong>Due:</strong> {{ new Date(v.dueAt).toLocaleDateString() }}</div>
                <div v-if="v.cveId"><strong>CVE:</strong> {{ v.cveId }}</div>
                <div v-if="v.cweId"><strong>CWE:</strong> {{ v.cweId }}</div>
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
          <v-card variant="elevated" elevation="2" class="vuln-card h-100">
            <v-card-title>{{ v.title }}</v-card-title>
            <v-card-text>
              <p class="text-caption text-secondary mb-2">{{ v.projectName }}</p>
              <p class="desc text-on-surface-variant mb-2">{{ v.description || 'No description provided.' }}</p>
              <div class="text-secondary text-caption">
                <div><strong>Severity:</strong> {{ v.severity }}</div>
                <div><strong>Status:</strong> {{ v.status }}</div>
                <div v-if="v.dueAt"><strong>Due:</strong> {{ new Date(v.dueAt).toLocaleDateString() }}</div>
                <div v-if="v.cveId"><strong>CVE:</strong> {{ v.cveId }}</div>
                <div v-if="v.cweId"><strong>CWE:</strong> {{ v.cweId }}</div>
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
          <v-card variant="elevated" elevation="2" class="vuln-card h-100">
            <v-card-title>{{ v.title }}</v-card-title>
            <v-card-text>
              <p class="text-caption text-secondary mb-2">{{ v.projectName }}</p>
              <p class="desc text-on-surface-variant mb-2">{{ v.description || 'No description provided.' }}</p>
              <div class="text-secondary text-caption">
                <div><strong>Severity:</strong> {{ v.severity }}</div>
                <div><strong>Status:</strong> {{ v.status }}</div>
                <div v-if="v.dueAt"><strong>Due:</strong> {{ new Date(v.dueAt).toLocaleDateString() }}</div>
                <div v-if="v.cveId"><strong>CVE:</strong> {{ v.cveId }}</div>
                <div v-if="v.cweId"><strong>CWE:</strong> {{ v.cweId }}</div>
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

.vuln-card {
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
}

.vuln-card:hover {
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
