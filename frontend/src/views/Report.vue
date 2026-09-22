<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  projectId?: string
  onSuccess?: () => void
}>()

const route = useRoute()
const authStore = useAuthStore()

const routeProjectId = route.params.projectId as string | undefined
const resolvedProjectId = ref(props.projectId || routeProjectId || '')

interface ProjectItem {
  id: string
  name: string
}

const projects = ref<ProjectItem[]>([])

const form = reactive({
  title: '',
  description: '',
  issueType: '',
  severity: '',
  status: '',
  dueAt: '',
  repository: '',
  commitHash: '',
  evidenceMachineId: '',
  evidenceLocalId: '',
  evidenceNote: '',
})

const error = ref('')
const success = ref('')

onMounted(async () => {
  if (!resolvedProjectId.value) {
    try {
      const res = await fetchWithAuth('/api/v1/userprojects/all')
      if (res.ok) {
        const data = await res.json()
        projects.value = data.data || []
      }
    } catch (err: unknown) {
      if (err instanceof Error) {
        console.error('Failed to load projects:', err.message)
      }
      authStore.logout()
    }
  }
})

const handleSubmit = async () => {
  error.value = ''
  success.value = ''

  if (!resolvedProjectId.value) {
    error.value = 'Please select a project first.'
    return
  }

  try {
    const payload = {
      title: form.title,
      description: form.description || null,
      type: form.issueType || null,
      severity: form.severity,
      status: form.status,
      dueAt: form.dueAt ? new Date(form.dueAt + 'T00:00:00').toISOString() : null,
      repository: form.repository || null,
      commitHash: form.commitHash || null,
      evidenceMachineId: form.evidenceMachineId || null,
      evidenceLocalId: form.evidenceLocalId || null,
      evidenceNote: form.evidenceNote || null,
    }

    const res = await fetchWithAuth(
      `/api/v1/issues/report/${resolvedProjectId.value}`,
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload),
      }
    )

    if (res.ok) {
      success.value = 'Issue reported successfully!'
      form.title = ''
      form.description = ''
      form.issueType = ''
      form.severity = ''
      form.status = ''
      form.dueAt = ''
      form.repository = ''
      form.commitHash = ''
      form.evidenceMachineId = ''
      form.evidenceLocalId = ''
      form.evidenceNote = ''
      if (props.onSuccess) props.onSuccess()
    } else {
      const data = await res.json()
      error.value = data.message || 'Failed to report issue'
    }
  } catch (err: unknown) {
    if (err instanceof Error) {
      console.error('Report fetch failed:', err.message)
    }
    error.value = 'Network or authentication error'
    authStore.logout()
  }
}

const issueTypeItems = [
  { title: 'Security', value: 'SECURITY' },
  { title: 'Reliability', value: 'RELIABILITY' },
  { title: 'Performance', value: 'PERFORMANCE' },
  { title: 'UX', value: 'UX' },
  { title: 'Other', value: 'OTHER' },
]

const severityItems = [
  { title: 'Low', value: 'LOW' },
  { title: 'Medium', value: 'MEDIUM' },
  { title: 'High', value: 'HIGH' },
  { title: 'Critical', value: 'CRITICAL' },
]

const statusItems = [
  { title: 'Reported', value: 'REPORTED' },
  { title: 'In Progress', value: 'IN_PROGRESS' },
  { title: 'Patched', value: 'PATCHED' },
  { title: 'Under Review', value: 'UNDER_REVIEW' },
  { title: 'Verified', value: 'VERIFIED' },
]

const projectItems = computed(() =>
  projects.value.map(p => ({ title: p.name, value: p.id }))
)
</script>

<template>
  <v-form @submit.prevent="handleSubmit" class="report-form mt-14">

    <!-- PROJECT DROPDOWN (ONLY WHEN NOT PRESELECTED) -->
    <v-select
      v-if="!props.projectId"
      v-model="resolvedProjectId"
      :items="projectItems"
      label="Project *"
      variant="outlined"
      density="comfortable"
      required
    />

    <v-text-field
      v-model="form.title"
      label="Title *"
      variant="outlined"
      density="comfortable"
      required
    />

    <v-text-field
      v-model="form.description"
      label="Description (Optional)"
      variant="outlined"
      density="comfortable"
    />

    <v-select
      v-model="form.issueType"
      :items="issueTypeItems"
      label="Issue Type"
      variant="outlined"
      density="comfortable"
      clearable
    />

    <v-select
      v-model="form.severity"
      :items="severityItems"
      label="Severity *"
      variant="outlined"
      density="comfortable"
      required
    />

    <v-select
      v-model="form.status"
      :items="statusItems"
      label="Status *"
      variant="outlined"
      density="comfortable"
      required
    />

    <v-text-field
      v-model="form.dueAt"
      label="Due At *"
      type="date"
      variant="outlined"
      density="comfortable"
      required
    />

    <v-text-field
      v-model="form.repository"
      label="Repository (Optional)"
      placeholder="e.g. github.com/org/repo"
      variant="outlined"
      density="comfortable"
    />

    <v-text-field
      v-model="form.commitHash"
      label="Commit / PR ID (Optional)"
      placeholder="e.g. abc1234 or PR#42"
      variant="outlined"
      density="comfortable"
    />

    <!-- Evidence Reference — opaque pointer to local machine storage -->
    <div class="mb-4">
      <div class="text-body-2 text-medium-emphasis mb-1">Evidence Reference (Optional)</div>
      <div class="text-caption text-secondary mb-3">
        Point to evidence stored on your local machine. The cloud never fetches or stores the evidence itself.
      </div>
      <v-text-field
        v-model="form.evidenceMachineId"
        label="Machine ID"
        placeholder="UUID of the machine holding the evidence"
        variant="outlined"
        density="comfortable"
        class="mb-2"
      />
      <v-text-field
        v-model="form.evidenceLocalId"
        label="Local Evidence ID"
        placeholder="UUID of the evidence on that machine"
        variant="outlined"
        density="comfortable"
        class="mb-2"
      />
      <v-text-field
        v-model="form.evidenceNote"
        label="Evidence Note (Optional)"
        placeholder="e.g. Recorded session in /var/evidence/sessions"
        variant="outlined"
        density="comfortable"
      />
    </div>

    <v-btn type="submit" color="info" block size="large" class="mt-2">
      Submit
    </v-btn>

    <v-alert v-if="error" type="error" density="compact" class="mt-3">{{ error }}</v-alert>
    <v-alert v-if="success" type="success" density="compact" class="mt-3">{{ success }}</v-alert>
  </v-form>
</template>

<style scoped>
.report-form {
  max-width: 560px;
  margin: 100px auto;
  padding: 30px;
  background-color: rgb(var(--v-theme-surface-variant));
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  font-family: Arial, sans-serif;
}

.text-mono {
  font-family: monospace;
}
</style>
