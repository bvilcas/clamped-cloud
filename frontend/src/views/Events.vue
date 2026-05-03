<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
import { useAuthStore } from '@/stores/auth'
import type { EventGroupDTO } from '@/dto/event-group-dto'

const authStore = useAuthStore()

interface ProjectItem {
  id: number
  name: string
}

const projects = ref<ProjectItem[]>([])
const selectedProjectId = ref<number | null>(null)
const eventGroups = ref<EventGroupDTO[]>([])
const envFilter = ref<string>('ALL')
const loading = ref(false)
const error = ref('')
const createIssueGroupId = ref<number | null>(null)
const createIssueLoading = ref(false)
const createIssueError = ref('')
const createIssueSuccess = ref('')

// Detail drawer
const drawerOpen = ref(false)
const selectedGroup = ref<EventGroupDTO | null>(null)

const envTabs = [
  { label: 'All', value: 'ALL' },
  { label: 'Production', value: 'PRODUCTION' },
  { label: 'Staging', value: 'STAGING' },
  { label: 'Dev', value: 'DEV' },
]

onMounted(async () => {
  try {
    const res = await fetchWithAuth('/api/v1/userprojects/all')
    if (res.ok) {
      const data = await res.json()
      projects.value = data.data || []
    }
  } catch (err: unknown) {
    if (err instanceof Error) console.error(err.message)
    authStore.logout()
  }
})

async function loadEvents() {
  if (!selectedProjectId.value) return
  loading.value = true
  error.value = ''
  try {
    const envParam = envFilter.value !== 'ALL' ? `?environment=${envFilter.value}` : ''
    const url = `/api/v1/projects/${selectedProjectId.value}/event-groups${envParam}`
    const res = await fetchWithAuth(url)
    if (res.ok) {
      eventGroups.value = await res.json()
    } else {
      error.value = 'Failed to load events'
    }
  } catch {
    error.value = 'Network error'
  } finally {
    loading.value = false
  }
}

async function toggleMute(group: EventGroupDTO) {
  try {
    const res = await fetchWithAuth(
      `/api/v1/event-groups/${group.id}/mute`,
      { method: 'PUT' }
    )
    if (res.ok) {
      const updated: EventGroupDTO = await res.json()
      const idx = eventGroups.value.findIndex(g => g.id === group.id)
      if (idx !== -1) {
        eventGroups.value[idx] = updated
        if (selectedGroup.value?.id === group.id) selectedGroup.value = updated
      }
    }
  } catch {
    console.error('Mute toggle failed')
  }
}

async function createIssue(group: EventGroupDTO) {
  createIssueGroupId.value = group.id
  createIssueLoading.value = true
  createIssueError.value = ''
  createIssueSuccess.value = ''
  try {
    const res = await fetchWithAuth(
      `/api/v1/issues/from-event-group/${group.id}`,
      { method: 'POST' }
    )
    if (res.ok) {
      const created = await res.json()
      createIssueSuccess.value = `Issue #${created.id} created successfully`
      const idx = eventGroups.value.findIndex(g => g.id === group.id)
      if (idx !== -1) {
        const updated = { ...group, linkedIssueId: created.id }
        eventGroups.value[idx] = updated
        if (selectedGroup.value?.id === group.id) selectedGroup.value = updated
      }
    } else {
      const data = await res.json().catch(() => ({}))
      createIssueError.value = (data as { message?: string }).message || 'Failed to create issue'
    }
  } catch {
    createIssueError.value = 'Network error'
  } finally {
    createIssueLoading.value = false
    createIssueGroupId.value = null
  }
}

function openDrawer(group: EventGroupDTO) {
  selectedGroup.value = group
  drawerOpen.value = true
}

const projectItems = computed(() =>
  projects.value.map(p => ({ title: p.name, value: p.id }))
)

function envColor(env: string) {
  if (env === 'PRODUCTION') return 'error'
  if (env === 'STAGING') return 'warning'
  return 'info'
}

function severityColor(sev: string) {
  if (sev === 'CRITICAL') return 'error'
  if (sev === 'HIGH') return 'deep-orange'
  if (sev === 'MEDIUM') return 'warning'
  return 'success'
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString()
}

function formatDateShort(iso: string) {
  return new Date(iso).toLocaleDateString()
}
</script>

<template>
  <v-container fluid class="pa-8">
    <h1 class="text-info mb-6">Events</h1>

    <v-select
      v-model="selectedProjectId"
      :items="projectItems"
      label="Select Project"
      variant="outlined"
      density="comfortable"
      class="mb-4"
      style="max-width: 400px"
      @update:model-value="loadEvents"
    />

    <template v-if="selectedProjectId">
      <!-- Environment filter tabs -->
      <v-tabs v-model="envFilter" class="mb-4" @update:model-value="loadEvents">
        <v-tab v-for="tab in envTabs" :key="tab.value" :value="tab.value">
          {{ tab.label }}
        </v-tab>
      </v-tabs>

      <v-alert v-if="error" type="error" density="compact" class="mb-4">{{ error }}</v-alert>
      <v-alert
        v-if="createIssueSuccess"
        type="success"
        density="compact"
        closable
        class="mb-4"
        @click:close="createIssueSuccess = ''"
      >
        {{ createIssueSuccess }}
      </v-alert>
      <v-alert
        v-if="createIssueError"
        type="error"
        density="compact"
        closable
        class="mb-4"
        @click:close="createIssueError = ''"
      >
        {{ createIssueError }}
      </v-alert>

      <v-progress-linear v-if="loading" indeterminate color="info" class="mb-4" />

      <p v-else-if="eventGroups.length === 0" class="text-secondary">No events found.</p>

      <v-table v-else density="comfortable" class="events-table">
        <thead>
          <tr>
            <th>Severity</th>
            <th>Message</th>
            <th>Source</th>
            <th>Environment</th>
            <th>Count</th>
            <th>Last Seen</th>
            <th>Linked Issue</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr
            v-for="group in eventGroups"
            :key="group.id"
            :class="{ 'muted-row': group.muted }"
            class="event-row"
            @click="openDrawer(group)"
          >
            <td @click.stop>
              <v-chip
                size="x-small"
                :color="severityColor(group.severity)"
                label
              >
                {{ group.severity }}
              </v-chip>
            </td>
            <td class="message-cell">
              <span class="exception-class" v-if="group.exceptionClass">{{ group.exceptionClass }}</span>
              <span :class="group.exceptionClass ? 'message-secondary' : ''">{{ group.message }}</span>
            </td>
            <td>
              <v-chip size="x-small" variant="outlined">{{ group.source }}</v-chip>
            </td>
            <td>
              <v-chip size="x-small" :color="envColor(group.environment)" label>
                {{ group.environment }}
              </v-chip>
            </td>
            <td class="count-cell">{{ group.count }}</td>
            <td class="text-caption">{{ formatDateShort(group.lastSeen) }}</td>
            <td>
              <span v-if="group.linkedIssueId" class="text-caption text-success">
                #{{ group.linkedIssueId }}
              </span>
              <span v-else class="text-caption text-secondary">—</span>
            </td>
            <td class="actions-cell" @click.stop>
              <v-btn
                size="x-small"
                :variant="group.muted ? 'tonal' : 'outlined'"
                :color="group.muted ? 'warning' : 'default'"
                class="mr-1"
                @click="toggleMute(group)"
              >
                {{ group.muted ? 'Unmute' : 'Mute' }}
              </v-btn>
              <v-btn
                v-if="!group.linkedIssueId"
                size="x-small"
                color="info"
                variant="tonal"
                :loading="createIssueGroupId === group.id && createIssueLoading"
                @click="createIssue(group)"
              >
                Create Issue
              </v-btn>
            </td>
          </tr>
        </tbody>
      </v-table>
    </template>

    <!-- Detail Drawer -->
    <v-navigation-drawer
      v-model="drawerOpen"
      location="right"
      width="480"
      temporary
    >
      <template v-if="selectedGroup">
        <div class="drawer-header pa-4">
          <div class="d-flex align-center justify-space-between mb-2">
            <v-chip
              :color="severityColor(selectedGroup.severity)"
              label
              size="small"
            >
              {{ selectedGroup.severity }}
            </v-chip>
            <v-btn icon="mdi-close" variant="text" size="small" @click="drawerOpen = false" />
          </div>

          <div class="exception-title mb-1" v-if="selectedGroup.exceptionClass">
            {{ selectedGroup.exceptionClass }}
          </div>
          <div class="drawer-message">{{ selectedGroup.message }}</div>
        </div>

        <v-divider />

        <div class="drawer-body pa-4">
          <!-- Meta info grid -->
          <div class="meta-grid mb-4">
            <div class="meta-item">
              <span class="meta-label">Environment</span>
              <v-chip size="x-small" :color="envColor(selectedGroup.environment)" label>
                {{ selectedGroup.environment }}
              </v-chip>
            </div>
            <div class="meta-item">
              <span class="meta-label">Source</span>
              <v-chip size="x-small" variant="outlined">{{ selectedGroup.source }}</v-chip>
            </div>
            <div class="meta-item">
              <span class="meta-label">Count</span>
              <strong>{{ selectedGroup.count }}</strong>
            </div>
            <div class="meta-item" v-if="selectedGroup.linkedIssueId">
              <span class="meta-label">Linked Issue</span>
              <span class="text-success">#{{ selectedGroup.linkedIssueId }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">First Seen</span>
              <span class="text-caption">{{ formatDate(selectedGroup.firstSeen) }}</span>
            </div>
            <div class="meta-item">
              <span class="meta-label">Last Seen</span>
              <span class="text-caption">{{ formatDate(selectedGroup.lastSeen) }}</span>
            </div>
          </div>

          <!-- Source location -->
          <template v-if="selectedGroup.sourceFile || selectedGroup.sourceMethod">
            <div class="section-label mb-1">Source Location</div>
            <div class="source-location pa-2 mb-4">
              <span v-if="selectedGroup.sourceMethod" class="source-method">{{ selectedGroup.sourceMethod }}</span>
              <span v-if="selectedGroup.sourceFile" class="source-file">
                {{ selectedGroup.sourceFile }}<span v-if="selectedGroup.sourceLine">:{{ selectedGroup.sourceLine }}</span>
              </span>
            </div>
          </template>

          <!-- Tags -->
          <template v-if="selectedGroup.tags">
            <div class="section-label mb-1">Tags</div>
            <div class="d-flex flex-wrap ga-1 mb-4">
              <v-chip
                v-for="tag in selectedGroup.tags.split(',')"
                :key="tag"
                size="x-small"
                variant="outlined"
              >
                {{ tag.trim() }}
              </v-chip>
            </div>
          </template>

          <!-- Fingerprint -->
          <div class="section-label mb-1">Fingerprint</div>
          <div class="fingerprint-row mb-4">{{ selectedGroup.fingerprint }}</div>

          <!-- Stacktrace -->
          <template v-if="selectedGroup.stacktrace">
            <div class="section-label mb-1">Stack Trace</div>
            <div class="stacktrace-block">
              <pre>{{ selectedGroup.stacktrace }}</pre>
            </div>
          </template>

          <!-- Actions -->
          <div class="d-flex ga-2 mt-4">
            <v-btn
              :variant="selectedGroup.muted ? 'tonal' : 'outlined'"
              :color="selectedGroup.muted ? 'warning' : 'default'"
              size="small"
              @click="toggleMute(selectedGroup)"
            >
              {{ selectedGroup.muted ? 'Unmute' : 'Mute' }}
            </v-btn>
            <v-btn
              v-if="!selectedGroup.linkedIssueId"
              color="info"
              variant="tonal"
              size="small"
              :loading="createIssueGroupId === selectedGroup.id && createIssueLoading"
              @click="createIssue(selectedGroup)"
            >
              Create Issue
            </v-btn>
          </div>
        </div>
      </template>
    </v-navigation-drawer>
  </v-container>
</template>

<style scoped>
.muted-row {
  opacity: 0.45;
}

.event-row {
  cursor: pointer;
}

.event-row:hover td {
  background: rgba(var(--v-theme-on-surface), 0.04);
}

.message-cell {
  max-width: 280px;
}

.exception-class {
  display: block;
  font-family: monospace;
  font-size: 0.78rem;
  font-weight: 600;
  color: rgb(var(--v-theme-error));
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.message-secondary {
  display: block;
  font-size: 0.8rem;
  color: rgb(var(--v-theme-on-surface-variant));
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.count-cell {
  font-weight: 600;
  min-width: 48px;
}

.actions-cell {
  white-space: nowrap;
}

/* Drawer styles */
.drawer-header {
  background: rgb(var(--v-theme-surface-variant));
}

.exception-title {
  font-family: monospace;
  font-size: 0.9rem;
  font-weight: 700;
  color: rgb(var(--v-theme-error));
}

.drawer-message {
  font-size: 0.92rem;
  color: rgb(var(--v-theme-on-surface));
  word-break: break-word;
}

.meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.meta-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.meta-label {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: rgb(var(--v-theme-on-surface-variant));
}

.section-label {
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: rgb(var(--v-theme-on-surface-variant));
  font-weight: 600;
}

.source-location {
  font-family: monospace;
  font-size: 0.82rem;
  background: rgba(var(--v-theme-on-surface), 0.05);
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.source-method {
  font-weight: 600;
  color: rgb(var(--v-theme-info));
}

.source-file {
  color: rgb(var(--v-theme-on-surface-variant));
}

.fingerprint-row {
  font-family: monospace;
  font-size: 0.75rem;
  color: rgb(var(--v-theme-on-surface-variant));
  word-break: break-all;
}

.stacktrace-block {
  background: rgba(var(--v-theme-on-surface), 0.05);
  border-radius: 6px;
  padding: 10px 12px;
  max-height: 340px;
  overflow-y: auto;
}

.stacktrace-block pre {
  font-family: monospace;
  font-size: 0.75rem;
  white-space: pre-wrap;
  word-break: break-word;
  color: rgb(var(--v-theme-on-surface));
  margin: 0;
}
</style>
