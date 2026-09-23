<script setup lang="ts">
// The member list for a project: invite somebody, change what they are, or
// take them off. All three are lead-only; a member sees the list read-only.
//
// Not wired up yet.
import { ref, onMounted } from 'vue'
import { fetchWithAuth } from '@/utils/fetchWithAuth'

type ProjectRole = 'LEAD' | 'MEMBER'

interface Member {
  userId: number
  firstName: string
  lastName: string
  email: string
  role: ProjectRole
}

const members = ref<Member[]>([])
const loading = ref(true)
const isLead = ref(false)
const inviteEmail = ref('')
const projectId = ref<number | null>(null)

const load = async () => {
  loading.value = true
  try {
    const res = await fetchWithAuth(`/api/v1/userprojects/${projectId.value}/members`)
    if (res.ok) members.value = await res.json()
  } finally {
    loading.value = false
  }
}

const invite = async () => {
  if (!inviteEmail.value) return
  await fetchWithAuth('/api/v1/userprojects/invite', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ projectId: projectId.value, email: inviteEmail.value }),
  })
  inviteEmail.value = ''
  await load()
}

const changeRole = async (userId: number, newRole: ProjectRole) => {
  await fetchWithAuth('/api/v1/userprojects/role', {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ projectId: projectId.value, userId, newRole }),
  })
  await load()
}

const remove = async (userId: number) => {
  await fetchWithAuth('/api/v1/userprojects/remove', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ projectId: projectId.value, userId }),
  })
  await load()
}

onMounted(load)
</script>

<template>
  <main class="app-content">
    <div class="title-row">
      <h1>Team</h1>
      <span class="text-secondary">{{ members.length }} members</span>
    </div>

    <!-- Lead only. A member can read the list but not change it. -->
    <form v-if="isLead" class="filter-bar" @submit.prevent="invite">
      <div class="form-field">
        <label for="invite-email">Invite by email</label>
        <input id="invite-email" v-model="inviteEmail" type="email" placeholder="name@university.edu">
      </div>
      <button type="submit" class="btn btn--info">Send Invite</button>
    </form>

    <p v-if="loading">Loading members...</p>

    <table v-else class="issue-table">
      <caption class="visually-hidden">Members of this project and the role each one holds.</caption>
      <thead>
        <tr>
          <th scope="col">Name</th>
          <th scope="col">Email</th>
          <th scope="col">Role</th>
          <th v-if="isLead" scope="col">Actions</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="m in members" :key="m.userId">
          <td>{{ m.firstName }} {{ m.lastName }}</td>
          <td>{{ m.email }}</td>
          <td><span class="chip">{{ m.role }}</span></td>
          <td v-if="isLead">
            <button
              type="button"
              class="btn btn--sm"
              @click="changeRole(m.userId, m.role === 'LEAD' ? 'MEMBER' : 'LEAD')"
            >
              {{ m.role === 'LEAD' ? 'Make Member' : 'Make Lead' }}
            </button>
            <button type="button" class="btn btn--sm btn--danger" @click="remove(m.userId)">
              Remove
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </main>
</template>
