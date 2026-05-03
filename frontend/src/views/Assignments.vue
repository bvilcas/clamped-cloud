<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const projectId = route.params.projectId as string

interface AssignmentEntry {
  userId: number
  firstname: string
  lastname: string
  role: string
  assignedAt: string
  selfAssigned: boolean
}

interface VulnWithAssignments {
  id: number
  title: string
  description: string
  cveId: string
  cweId: string
  severity: string
  status: string
  reportedAt: string
  dueAt: string
  patchedAt: string
  verifiedAt: string
  repository: string
  commitHash: string
  assignments: AssignmentEntry[]
}

interface Member {
  id: number
  firstname: string
  lastname: string
  projectRole: string
}

const myId = ref<number | null>(null)
const myRole = ref('')
const vulns = ref<VulnWithAssignments[]>([])
const members = ref<Member[]>([])
const loading = ref(true)

const filterStatus = ref('')
const filterSeverity = ref('')
const filterMine = ref(false)

const assignDialog = ref(false)
const assignTargetVuln = ref<VulnWithAssignments | null>(null)
const assignMemberId = ref<number | null>(null)
const assignRole = ref('')
const assignLoading = ref(false)
const assignError = ref('')

const actionLoading = ref<Record<string, boolean>>({})

const statusOptions = [
  { title: 'All Statuses', value: '' },
  { title: 'Reported', value: 'REPORTED' },
  { title: 'In Progress', value: 'IN_PROGRESS' },
  { title: 'Patched', value: 'PATCHED' },
  { title: 'Under Review', value: 'UNDER_REVIEW' },
  { title: 'Verified', value: 'VERIFIED' },
]

const severityOptions = [
  { title: 'All Severities', value: '' },
  { title: 'Low', value: 'LOW' },
  { title: 'Medium', value: 'MEDIUM' },
  { title: 'High', value: 'HIGH' },
  { title: 'Critical', value: 'CRITICAL' },
]

const filteredVulns = computed(() =>
  vulns.value.filter(v => {
    if (filterStatus.value && v.status !== filterStatus.value) return false
    if (filterSeverity.value && v.severity !== filterSeverity.value) return false
    if (filterMine.value && !v.assignments.some(a => a.userId === myId.value)) return false
    return true
  })
)

const isAssigned = (v: VulnWithAssignments) =>
  v.assignments.some(a => a.userId === myId.value)

const myAssignment = (v: VulnWithAssignments) =>
  v.assignments.find(a => a.userId === myId.value)

const canSelfAssign = (v: VulnWithAssignments) => {
  if (isAssigned(v)) return false
  return myRole.value === 'PROGRAMMER' || myRole.value === 'LEAD' || myRole.value === 'TESTER'
}

const severityClass = (s: string) => `severity-${s?.toLowerCase()}`
const statusClass = (s: string) => `status-${s?.toLowerCase().replace('_', '-')}`

const roleLabel = (role: string) => {
  if (role === 'REPORTER') return 'Reporter'
  if (role === 'ASSIGNEE') return 'Assignee'
  if (role === 'VERIFIER') return 'Verifier'
  return role
}

const roleChipColor = (role: string) => {
  if (role === 'REPORTER') return 'secondary'
  if (role === 'ASSIGNEE') return 'info'
  if (role === 'VERIFIER') return 'success'
  return 'surface-variant'
}

const autoRole = computed(() => {
  if (!assignMemberId.value) return ''
  const m = members.value.find(m => m.id === assignMemberId.value)
  if (!m) return ''
  if (m.projectRole === 'PROGRAMMER') return 'ASSIGNEE'
  if (m.projectRole === 'TESTER') return 'VERIFIER'
  return '' // LEAD → manual selection
})

const leadRoleOptions = [
  { title: 'Reporter', value: 'REPORTER' },
  { title: 'Assignee', value: 'ASSIGNEE' },
  { title: 'Verifier', value: 'VERIFIER' },
]

const effectiveAssignRole = computed(() => autoRole.value || assignRole.value)

const loadData = async () => {
  try {
    loading.value = true
    const [meRes, projectRes, membersRes, vulnsRes] = await Promise.all([
      fetchWithAuth('/api/v1/users/me'),
      fetchWithAuth(`/api/v1/userprojects/${projectId}`),
      fetchWithAuth(`/api/v1/userprojects/members/${projectId}`),
      fetchWithAuth(`/api/v1/uservulns/project-assignments/${projectId}`),
    ])
    if (meRes.ok) {
      const me = await meRes.json()
      myId.value = me.id
    }
    if (projectRes.ok) {
      const proj = await projectRes.json()
      myRole.value = proj.data?.myRole ?? ''
    }
    if (membersRes.ok) {
      const mem = await membersRes.json()
      members.value = mem.data || []
    }
    if (vulnsRes.ok) {
      const v = await vulnsRes.json()
      vulns.value = v.data || []
    }
  } catch {
    authStore.logout()
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const selfAssign = async (v: VulnWithAssignments) => {
  const key = `assign-${v.id}`
  actionLoading.value[key] = true
  try {
    const res = await fetchWithAuth('/api/v1/uservulns/self-assign', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ vulnId: v.id, projectId: Number(projectId) }),
    })
    if (res.ok) await loadData()
    else {
      const err = await res.json()
      alert(err.message || 'Self-assign failed.')
    }
  } catch {
    alert('Network error.')
  } finally {
    actionLoading.value[key] = false
  }
}

const selfRevoke = async (v: VulnWithAssignments) => {
  const key = `revoke-${v.id}`
  actionLoading.value[key] = true
  try {
    const res = await fetchWithAuth('/api/v1/uservulns/self-revoke', {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ vulnId: v.id, projectId: Number(projectId) }),
    })
    if (res.ok) await loadData()
    else {
      const err = await res.json()
      alert(err.message || 'Revoke failed.')
    }
  } catch {
    alert('Network error.')
  } finally {
    actionLoading.value[key] = false
  }
}

const openAssignDialog = (v: VulnWithAssignments) => {
  assignTargetVuln.value = v
  assignMemberId.value = null
  assignRole.value = ''
  assignError.value = ''
  assignDialog.value = true
}

const submitLeadAssign = async () => {
  if (!assignTargetVuln.value || !assignMemberId.value || !effectiveAssignRole.value) {
    assignError.value = 'Please select a member and a role.'
    return
  }
  assignLoading.value = true
  assignError.value = ''
  try {
    const res = await fetchWithAuth('/api/v1/uservulns/assign', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: assignMemberId.value,
        vulnId: assignTargetVuln.value.id,
        projectId: Number(projectId),
        role: effectiveAssignRole.value,
      }),
    })
    if (res.ok) {
      assignDialog.value = false
      await loadData()
    } else {
      const err = await res.json()
      assignError.value = err.message || 'Assignment failed.'
    }
  } catch {
    assignError.value = 'Network error.'
  } finally {
    assignLoading.value = false
  }
}

const leadUnassign = async (v: VulnWithAssignments, entry: AssignmentEntry) => {
  const key = `unassign-${v.id}-${entry.userId}`
  actionLoading.value[key] = true
  try {
    const res = await fetchWithAuth('/api/v1/uservulns/unassign', {
      method: 'DELETE',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: entry.userId, vulnId: v.id, projectId: Number(projectId) }),
    })
    if (res.ok) await loadData()
    else {
      const err = await res.json()
      alert(err.message || 'Unassign failed.')
    }
  } catch {
    alert('Network error.')
  } finally {
    actionLoading.value[key] = false
  }
}
</script>

<template>
  <v-container class="pa-8">
    <div class="d-flex align-center mb-1 ga-2">
      <v-btn icon="mdi-arrow-left" variant="text" size="small" @click="router.back()" />
      <h1 class="text-info">Assignments</h1>
    </div>
    <p class="text-secondary mb-6">View and manage vulnerability assignments for this project.</p>

    <!-- Filter bar -->
    <v-card variant="elevated" elevation="1" class="mb-6 pa-4">
      <div class="d-flex flex-wrap ga-3 align-center">
        <v-select
          v-model="filterStatus"
          :items="statusOptions"
          item-title="title"
          item-value="value"
          label="Status"
          variant="outlined"
          density="compact"
          hide-details
          style="min-width: 160px; max-width: 190px"
        />
        <v-select
          v-model="filterSeverity"
          :items="severityOptions"
          item-title="title"
          item-value="value"
          label="Severity"
          variant="outlined"
          density="compact"
          hide-details
          style="min-width: 160px; max-width: 190px"
        />
        <v-switch
          v-model="filterMine"
          label="My assignments only"
          color="info"
          hide-details
          density="compact"
          class="ml-1"
        />
        <v-spacer />
        <span class="text-secondary text-caption">
          {{ filteredVulns.length }}&nbsp;/&nbsp;{{ vulns.length }} vulnerabilities
        </span>
      </div>
    </v-card>

    <!-- Loading -->
    <div v-if="loading" class="d-flex justify-center py-12">
      <v-progress-circular indeterminate color="info" />
    </div>

    <!-- Empty state -->
    <p v-else-if="filteredVulns.length === 0" class="text-secondary">
      No vulnerabilities match the selected filters.
    </p>

    <!-- Vuln cards -->
    <div v-else>
      <v-card
        v-for="v in filteredVulns"
        :key="v.id"
        variant="elevated"
        elevation="2"
        class="vuln-card mb-4"
      >
        <v-card-text>
          <!-- Header -->
          <div class="d-flex align-start justify-space-between flex-wrap ga-3 mb-3">
            <div class="flex-grow-1">
              <p class="text-subtitle-1 font-weight-semibold mb-2">{{ v.title }}</p>
              <div class="d-flex ga-2 flex-wrap">
                <v-chip :class="severityClass(v.severity)" size="small" label>
                  {{ v.severity }}
                </v-chip>
                <v-chip :class="statusClass(v.status)" size="small" label>
                  {{ v.status.replace(/_/g, ' ') }}
                </v-chip>
                <v-chip
                  v-if="v.dueAt"
                  size="small"
                  label
                  color="surface-variant"
                  prepend-icon="mdi-calendar-clock"
                >
                  Due {{ new Date(v.dueAt).toLocaleDateString() }}
                </v-chip>
                <v-chip v-if="v.cveId" size="small" label color="surface-variant">
                  {{ v.cveId }}
                </v-chip>
              </div>
            </div>

            <!-- Actions -->
            <div class="d-flex ga-2 flex-wrap align-start">
              <v-btn
                v-if="canSelfAssign(v)"
                size="small"
                color="info"
                variant="tonal"
                prepend-icon="mdi-account-plus-outline"
                :loading="actionLoading[`assign-${v.id}`]"
                @click="selfAssign(v)"
              >
                Self-Assign
              </v-btn>

              <v-btn
                v-if="isAssigned(v) && myAssignment(v)?.role !== 'REPORTER'"
                size="small"
                color="warning"
                variant="tonal"
                prepend-icon="mdi-account-minus-outline"
                :loading="actionLoading[`revoke-${v.id}`]"
                @click="selfRevoke(v)"
              >
                Revoke
              </v-btn>

              <v-btn
                v-if="myRole === 'LEAD'"
                size="small"
                color="secondary"
                variant="tonal"
                prepend-icon="mdi-account-arrow-right-outline"
                @click="openAssignDialog(v)"
              >
                Assign Member
              </v-btn>
            </div>
          </div>

          <!-- Description -->
          <p v-if="v.description" class="text-secondary text-body-2 desc mb-3">
            {{ v.description }}
          </p>

          <v-divider class="mb-3" />

          <!-- Assignments list -->
          <p class="text-caption text-secondary font-weight-medium mb-2">CURRENT ASSIGNMENTS</p>

          <p v-if="v.assignments.length === 0" class="text-caption text-secondary">
            No one assigned yet.
          </p>

          <div v-else class="d-flex flex-wrap ga-2">
            <div
              v-for="entry in v.assignments"
              :key="`${v.id}-${entry.userId}`"
              class="assignment-entry d-flex align-center ga-1"
            >
              <v-chip
                :color="roleChipColor(entry.role)"
                size="small"
                variant="tonal"
                :prepend-icon="entry.userId === myId ? 'mdi-account-circle' : 'mdi-account-outline'"
              >
                {{ entry.firstname }} {{ entry.lastname }}
                <span class="ml-1 text-caption opacity-70">&middot; {{ roleLabel(entry.role) }}</span>
              </v-chip>

              <v-btn
                v-if="myRole === 'LEAD'"
                icon="mdi-close"
                size="x-small"
                variant="text"
                color="error"
                :loading="actionLoading[`unassign-${v.id}-${entry.userId}`]"
                @click="leadUnassign(v, entry)"
              />
            </div>
          </div>
        </v-card-text>
      </v-card>
    </div>

    <!-- Lead Assign Dialog -->
    <v-dialog v-model="assignDialog" max-width="440">
      <v-card>
        <v-card-title class="pt-5 px-5">Assign Member to Vulnerability</v-card-title>
        <v-card-subtitle v-if="assignTargetVuln" class="px-5 pb-1">
          {{ assignTargetVuln.title }}
        </v-card-subtitle>
        <v-card-text class="px-5 pt-4">
          <v-select
            v-model="assignMemberId"
            :items="members"
            :item-title="(m: Member) => `${m.firstname} ${m.lastname} (${m.projectRole})`"
            item-value="id"
            label="Select Member"
            variant="outlined"
            density="comfortable"
            class="mb-3"
          />

          <div v-if="assignMemberId && autoRole" class="mb-3">
            <v-alert type="info" variant="tonal" density="compact">
              Will be assigned as <strong>{{ roleLabel(autoRole) }}</strong> based on their project role.
            </v-alert>
          </div>

          <v-select
            v-if="assignMemberId && !autoRole"
            v-model="assignRole"
            :items="leadRoleOptions"
            item-title="title"
            item-value="value"
            label="Assign Role"
            variant="outlined"
            density="comfortable"
          />

          <v-alert v-if="assignError" type="error" density="compact" class="mt-2">
            {{ assignError }}
          </v-alert>
        </v-card-text>
        <v-card-actions class="px-5 pb-5">
          <v-spacer />
          <v-btn variant="text" color="secondary" @click="assignDialog = false">Cancel</v-btn>
          <v-btn
            color="info"
            :loading="assignLoading"
            :disabled="!assignMemberId || !effectiveAssignRole"
            @click="submitLeadAssign"
          >
            Assign
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<style scoped>
.vuln-card {
  transition: box-shadow 0.15s ease;
}

.desc {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.assignment-entry {
  display: inline-flex;
  align-items: center;
}
</style>
