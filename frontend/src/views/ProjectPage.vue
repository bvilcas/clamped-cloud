<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
import { useAuthStore } from '@/stores/auth'
import Report from '@/views/Report.vue'
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const projectId = route.params.projectId as string

interface ProjectData {
  name: string
  description: string
  createdAt: string
  updatedAt: string
  myRole: string
}

interface Member {
  id: string
  firstname: string
  lastname: string
  projectRole: string
}

interface IssueItem {
  id: string
  title: string
  severity: string
  status: string
}

const project = ref<ProjectData | null>(null)
const members = ref<Member[]>([])
const vulnerabilities = ref<IssueItem[]>([])
const loadingVulns = ref(true)
const showReportForm = ref(false)
const newMemberRole = ref('')
const memberActionLoading = ref(false)

interface UserSuggestion { id: number; email: string; firstname: string; lastname: string }
const emailQuery = ref('')
const emailSuggestions = ref<UserSuggestion[]>([])
const selectedUser = ref<UserSuggestion | null>(null)
const emailSearchLoading = ref(false)
let emailSearchTimer: ReturnType<typeof setTimeout> | null = null

const clearEmailSelection = () => {
  selectedUser.value = null
  emailSuggestions.value = []
  emailQuery.value = ''
}

const selectUser = (u: UserSuggestion) => {
  selectedUser.value = u
  emailSuggestions.value = []
  emailQuery.value = u.email
}

watch(emailQuery, (query) => {
  if (selectedUser.value) return
  if (emailSearchTimer) clearTimeout(emailSearchTimer)
  if (!query || query.length < 2) { emailSuggestions.value = []; return }
  emailSearchTimer = setTimeout(async () => {
    emailSearchLoading.value = true
    try {
      const res = await fetchWithAuth(
        `/api/v1/users/search?email=${encodeURIComponent(query)}`
      )
      if (res.ok) {
        const data = await res.json()
        emailSuggestions.value = data.data || []
      }
    } catch (err) {
      console.error('User search failed:', err)
    } finally {
      emailSearchLoading.value = false
    }
  }, 300)
})
const role = ref('')

const memberRoleItems = [
  { title: 'Tester', value: 'TESTER' },
  { title: 'Programmer', value: 'PROGRAMMER' },
  { title: 'Lead', value: 'LEAD' },
]

const severityClass = (s: string) => `severity-${s.toLowerCase()}`
const statusClass = (s: string) => `status-${s.toLowerCase()}`

const loadProject = async () => {
  try {
    const res = await fetchWithAuth(
      `/api/v1/userprojects/${projectId}`
    )
    if (!res.ok) { authStore.logout(); return }
    const data = await res.json()
    project.value = data.data
    role.value = data.data.myRole
  } catch (err) {
    authStore.logout()
  }
}

const loadMembers = async () => {
  try {
    const res = await fetchWithAuth(
      `/api/v1/userprojects/members/${projectId}`
    )
    const data = await res.json()
    members.value = data.data || []
  } catch (err) {
    authStore.logout()
  }
}

const loadVulnerabilities = async () => {
  try {
    const res = await fetchWithAuth(
      `/api/v1/issues/all/${projectId}`
    )
    if (res.ok) {
      vulnerabilities.value = await res.json()
    }
  } catch (err) {
    authStore.logout()
  } finally {
    loadingVulns.value = false
  }
}

onMounted(() => {
  loadProject()
  loadMembers()
  loadVulnerabilities()
  localStorage.setItem('lastProjectId', projectId)
})

const handleSelfRemove = async () => {
  try {
    const res = await fetchWithAuth(
      `/api/v1/userprojects/self-remove/validate/${projectId}`,
      { method: 'POST' }
    )
    const data = await res.json()
    if (data.data?.requiresConfirmation) { alert(data.data.message); return }
    await fetchWithAuth(
      `/api/v1/userprojects/self-remove/${projectId}`,
      { method: 'DELETE' }
    )
    router.push('/projects')
  } catch (err) {
    authStore.logout()
  }
}

const handleDeleteProject = async () => {
  const confirmed = window.confirm('Are you sure? This will permanently delete the entire project.')
  if (!confirmed) return
  try {
    await fetchWithAuth(
      `/api/v1/projects/delete/${projectId}`,
      { method: 'DELETE' }
    )
    router.push('/projects')
  } catch (err) {
    authStore.logout()
  }
}

const handleDeleteVulnerability = async (vulnerabilityId: string) => {
  const confirmed = window.confirm('Delete this issue?')
  if (!confirmed) return
  try {
    const res = await fetchWithAuth(
      `/api/v1/issues/delete/${projectId}/${vulnerabilityId}`,
      { method: 'DELETE' }
    )
    if (!res.ok) { const data = await res.json(); alert(data.message || 'Failed to delete issue.'); return }
    loadVulnerabilities()
  } catch (err: unknown) {
    if (err instanceof Error) console.error('Delete issue failed:', err.message)
    alert('Network or authentication error while deleting.')
    authStore.logout()
  }
}

const handleStatusChange = async (issueId: string, newStatus: string) => {
  try {
    const res = await fetchWithAuth(
      '/api/v1/issues/status',
      {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ projectId: parseInt(projectId), issueId: parseInt(issueId), newStatus })
      }
    )
    if (!res.ok) { const data = await res.json(); alert(data.message || 'Failed to update issue status.'); return }
    loadVulnerabilities()
  } catch (err: unknown) {
    if (err instanceof Error) console.error('Status change failed:', err.message)
    alert('Network or authentication error while updating status.')
    authStore.logout()
  }
}

const handleAddMember = async () => {
  if (!selectedUser.value) { alert('Select a user by searching their email.'); return }
  if (!newMemberRole.value) { alert('Select a role.'); return }
  try {
    memberActionLoading.value = true
    const res = await fetchWithAuth(
      '/api/v1/userprojects/members/add',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ projectId, userId: selectedUser.value.id, role: newMemberRole.value })
      }
    )
    const data = await res.json()
    if (data.data?.requiresConfirmation) {
      const confirmed = window.confirm(data.data.message)
      if (!confirmed) return
      await fetchWithAuth(
        '/api/v1/userprojects/members/add',
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ projectId, userId: selectedUser.value.id, role: newMemberRole.value })
        }
      )
    }
    selectedUser.value = null
    emailSuggestions.value = []
    newMemberRole.value = 'TESTER'
    loadMembers()
  } catch (err) {
    alert('Failed to add member.')
  } finally {
    memberActionLoading.value = false
  }
}

const handleRemoveMember = async (userId: string) => {
  try {
    const validateRes = await fetchWithAuth(
      '/api/v1/userprojects/members/remove/validate',
      { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ projectId, userId }) }
    )
    const validateData = await validateRes.json()
    if (validateData.data?.requiresConfirmation) {
      const confirmed = window.confirm(validateData.data.message)
      if (!confirmed) return
    }
    await fetchWithAuth(
      '/api/v1/userprojects/members/remove',
      { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ projectId, userId }) }
    )
    loadMembers()
  } catch (err) {
    alert('Failed to remove member.')
  }
}

const handleOpenAssignments = () => {
  router.push(`/project/${projectId}/assignments`)
}

const roleDialog = ref({ open: false, memberId: '', memberName: '', currentRole: '', newRole: '' })

const openRoleDialog = (m: Member) => {
  roleDialog.value = {
    open: true,
    memberId: m.id,
    memberName: `${m.firstname} ${m.lastname}`,
    currentRole: m.projectRole,
    newRole: m.projectRole,
  }
}

const confirmRoleChange = async () => {
  const { memberId, newRole, currentRole } = roleDialog.value
  if (newRole === currentRole) { roleDialog.value.open = false; return }
  try {
    const res = await fetchWithAuth(
      '/api/v1/userprojects/change',
      {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ projectId, userId: memberId, newRole })
      }
    )
    if (!res.ok) { const data = await res.json(); alert(data.message || 'Failed to update role.'); return }
    roleDialog.value.open = false
    loadMembers()
  } catch (err) {
    alert('Failed to update role.')
  }
}
</script>

<template>
  <v-container class="pa-8">
    <p v-if="!project" class="text-secondary">Loading...</p>

    <template v-else>
      <!-- Header -->
      <div class="d-flex justify-space-between align-start mb-6">
        <div>
          <h1 class="text-info mb-1">{{ project.name }}</h1>
          <p class="text-secondary mb-2">{{ project.description || 'No description provided.' }}</p>
          <div class="d-flex ga-4 text-secondary text-caption">
            <span>Created: {{ new Date(project.createdAt).toLocaleDateString() }}</span>
            <span>Last Updated: {{ project.updatedAt ? new Date(project.updatedAt).toLocaleDateString() : '—' }}</span>
          </div>
        </div>
        <div class="d-flex flex-column align-end ga-2">
          <v-chip variant="outlined" color="secondary">My Role: {{ role }}</v-chip>
          <div class="d-flex ga-2">
            <template v-if="role === 'LEAD'">
              <v-btn color="info" @click="router.push(`/project/update/${projectId}`)">Update Project</v-btn>
              <v-btn color="error" @click="handleDeleteProject">Delete Project</v-btn>
            </template>
            <v-btn color="error" variant="outlined" @click="handleSelfRemove">Leave Project</v-btn>
          </div>
        </div>
      </div>

      <!-- Two-column layout -->
      <v-row>
        <!-- Left: Vulnerabilities -->
        <v-col cols="12" md="6">
          <v-card variant="elevated" elevation="2">
            <v-card-title class="d-flex justify-space-between align-center">
              Issues
              <v-btn color="info" size="small" @click="showReportForm = !showReportForm">
                + Report
              </v-btn>
            </v-card-title>
            <v-card-text>
              <div v-if="showReportForm" class="mb-4">
                <Report :project-id="projectId" :on-success="() => { loadVulnerabilities(); showReportForm = false }" />
              </div>

              <p v-if="loadingVulns" class="text-secondary">Loading issues...</p>
              <p v-else-if="vulnerabilities.length === 0" class="text-secondary">No issues reported yet.</p>
              <template v-else v-for="(v, i) in vulnerabilities" :key="v.id">
                <v-divider v-if="i > 0" class="my-2" />
                <div class="d-flex flex-column ga-1 py-1">
                  <div class="d-flex justify-space-between align-center">
                    <strong>{{ v.title }}</strong>
                    <span :class="['severity-badge', severityClass(v.severity)]">{{ v.severity }}</span>
                  </div>
                  <div class="d-flex justify-space-between align-center">
                    <span :class="['text-caption font-weight-bold', statusClass(v.status)]">{{ v.status }}</span>
                    <div class="d-flex ga-1">
                      <v-btn size="x-small" color="success" variant="tonal" @click="handleStatusChange(v.id, 'PATCHED')">Patch</v-btn>
                      <v-btn size="x-small" color="secondary" variant="tonal" @click="handleStatusChange(v.id, 'VERIFIED')">Verify</v-btn>
                      <v-btn v-if="role === 'LEAD'" size="x-small" color="error" variant="outlined" @click="handleDeleteVulnerability(v.id)">Delete</v-btn>
                    </div>
                  </div>
                </div>
              </template>
            </v-card-text>
          </v-card>
        </v-col>

        <!-- Right: Members + Assignments -->
        <v-col cols="12" md="6">
          <!-- Members -->
          <v-card variant="elevated" elevation="2" class="mb-6">
            <v-card-title>Project Members</v-card-title>
            <v-card-text>
              <template v-for="(m, i) in members" :key="m.id">
                <v-divider v-if="i > 0" class="my-1" />
                <div class="d-flex align-center justify-space-between py-2">
                  <div>
                    <span class="font-weight-bold">{{ m.firstname }} {{ m.lastname }}</span>
                    <span class="text-secondary text-caption ml-2">{{ m.projectRole }}</span>
                  </div>
                  <div v-if="role === 'LEAD'" class="d-flex ga-1">
                    <v-btn size="x-small" variant="outlined" @click="openRoleDialog(m)">Change Role</v-btn>
                    <v-btn size="x-small" color="error" variant="outlined" @click="handleRemoveMember(m.id)">Remove</v-btn>
                  </div>
                </div>
              </template>

              <template v-if="role === 'LEAD'">
                <v-divider class="my-3" />
                <p class="text-secondary text-caption mb-2">Add Member</p>
                <div class="d-flex flex-column ga-2">
                  <div style="position: relative;">
                    <v-text-field
                      v-model="emailQuery"
                      label="Search by email"
                      variant="outlined"
                      density="compact"
                      hide-details
                      :loading="emailSearchLoading"
                      clearable
                      autocomplete="off"
                      @click:clear="clearEmailSelection"
                    />
                    <v-card
                      v-if="emailSuggestions.length > 0"
                      variant="elevated"
                      elevation="8"
                      style="position: absolute; width: 100%; z-index: 200; top: 100%; max-height: 200px; overflow-y: auto;"
                    >
                      <v-list density="compact" nav>
                        <v-list-item
                          v-for="u in emailSuggestions"
                          :key="u.id"
                          :title="u.email"
                          :subtitle="`${u.firstname} ${u.lastname}`"
                          @click="selectUser(u)"
                        />
                      </v-list>
                    </v-card>
                  </div>
                  <v-chip v-if="selectedUser" size="small" color="info" closable @click:close="clearEmailSelection">
                    {{ selectedUser.firstname }} {{ selectedUser.lastname }} ({{ selectedUser.email }})
                  </v-chip>
                  <v-select v-model="newMemberRole" :items="memberRoleItems" label="Role" variant="outlined" density="compact" hide-details />
                  <div>
                    <v-btn color="info" @click="handleAddMember" :disabled="memberActionLoading || !selectedUser">
                      {{ memberActionLoading ? 'Adding...' : 'Add Member' }}
                    </v-btn>
                  </div>
                </div>
              </template>
            </v-card-text>
          </v-card>

          <!-- Assignments -->
          <v-card variant="elevated" elevation="2">
            <v-card-title>Assignments</v-card-title>
            <v-card-text>
              <p class="text-secondary mb-3">View, self-assign, and manage issue roles.</p>
              <v-btn color="info" @click="handleOpenAssignments">Open Assignments</v-btn>
            </v-card-text>
          </v-card>
        </v-col>
      </v-row>
    </template>

    <!-- Role Change Dialog -->
    <v-dialog v-model="roleDialog.open" max-width="360">
      <v-card>
        <v-card-title>Change Role</v-card-title>
        <v-card-text>
          <p class="text-secondary mb-4">
            <strong>{{ roleDialog.memberName }}</strong> is currently
            <strong>{{ roleDialog.currentRole }}</strong>.
          </p>
          <v-select
            v-model="roleDialog.newRole"
            :items="memberRoleItems"
            label="New Role"
            variant="outlined"
            density="comfortable"
          />
        </v-card-text>
        <v-card-actions class="justify-end ga-2">
          <v-btn variant="text" @click="roleDialog.open = false">Cancel</v-btn>
          <v-btn color="info" :disabled="roleDialog.newRole === roleDialog.currentRole" @click="confirmRoleChange">
            Confirm
          </v-btn>
        </v-card-actions>
      </v-card>
    </v-dialog>
  </v-container>
</template>

<style scoped>
.severity-badge {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 0.75rem;
  font-weight: bold;
}

.severity-low    { background: rgb(var(--v-theme-severity-low));      color: rgb(var(--v-theme-on-severity-low)); }
.severity-medium { background: rgb(var(--v-theme-severity-medium));   color: rgb(var(--v-theme-on-severity-medium)); }
.severity-high   { background: rgb(var(--v-theme-severity-high));     color: rgb(var(--v-theme-on-severity-high)); }
.severity-critical { background: rgb(var(--v-theme-severity-critical)); color: rgb(var(--v-theme-on-severity-critical)); }

.status-reported    { color: rgb(var(--v-theme-info)); }
.status-in_progress { color: rgb(var(--v-theme-status-in-progress)); }
.status-patched     { color: rgb(var(--v-theme-status-patched)); }
.status-verified    { color: rgb(var(--v-theme-status-verified)); }
.status-under_review { color: rgb(var(--v-theme-warning)); }
</style>
