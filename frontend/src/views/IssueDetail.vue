<script setup lang="ts">
// One issue. Replaces the old per-project Assignments page: the actions that
// lived there belong on the ticket they act on.
//
// Laid out after prototype/detail.html in clamped-redesign: the actions in one
// bar under the title, then the description and event payload on the left with
// the facts, the roles and the progress trail on the right.
//
// Not wired up yet. The API calls below are the ones it will need.
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
import type { IssueDTO } from '@/dto/issue-dto'

const route = useRoute()
const issue = ref<IssueDTO | null>(null)
const loading = ref(true)

// Whether this user may take the verifier role. The server decides: anyone who
// has ever held ASSIGNEE on this issue is refused, and the button explains why
// instead of disappearing.
const canVerify = ref(false)
const verifyBlockedReason = ref('')

const load = async () => {
  loading.value = true
  try {
    const res = await fetchWithAuth(`/api/v1/issues/${route.params.issueId}`)
    if (res.ok) issue.value = await res.json()
  } finally {
    loading.value = false
  }
}

const advance = async (status: string) => {
  await fetchWithAuth(`/api/v1/issues/${route.params.issueId}/status`, {
    method: 'PATCH',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ status }),
  })
  await load()
}

const selfAssign = async (role: 'ASSIGNEE' | 'VERIFIER') => {
  await fetchWithAuth('/api/v1/userissues/self-assign', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ issueId: route.params.issueId, roleInIssue: role }),
  })
  await load()
}

const revokeMyRole = async () => {
  await fetchWithAuth('/api/v1/userissues/self-revoke', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ issueId: route.params.issueId }),
  })
  await load()
}

onMounted(load)
</script>

<template>
  <main class="app-content">
    <p v-if="loading">Loading issue...</p>

    <template v-else-if="issue">
      <nav class="breadcrumb" aria-label="Breadcrumb">
        <ol>
          <li><RouterLink to="/dashboard">Dashboard</RouterLink></li>
          <li><RouterLink to="/issues">My Issues</RouterLink></li>
          <li><span aria-current="page">{{ issue.id }}</span></li>
        </ol>
      </nav>

      <div class="title-row">
        <div>
          <h1>{{ issue.title }}</h1>
          <div class="chip-row">
            <span class="chip">{{ issue.severity }}</span>
            <span class="chip">{{ issue.status }}</span>
          </div>
        </div>
        <div class="btn-row">
          <button type="button" class="btn">Edit Details</button>
          <RouterLink class="btn" to="/issues">Back to My Issues</RouterLink>
        </div>
      </div>

      <!-- Everything you can do, in one bar. Delete sits apart from the rest. -->
      <div class="action-bar">
        <div class="action-bar__group">
          <button type="button" class="btn btn--info" @click="advance('UNDER_REVIEW')">
            Move to Under Review
          </button>
          <button type="button" class="btn" @click="selfAssign('ASSIGNEE')">Take as Assignee</button>
          <button type="button" class="btn" @click="revokeMyRole">Revoke my role</button>
          <button
            type="button"
            class="btn"
            :disabled="!canVerify"
            @click="selfAssign('VERIFIER')"
          >
            Take as Verifier
          </button>
        </div>

        <div class="action-bar__group">
          <button type="button" class="btn btn--danger">Delete</button>
        </div>

        <!-- The two-person rule, said in the one place it applies. -->
        <p v-if="!canVerify" class="action-bar__note">{{ verifyBlockedReason }}</p>
      </div>

      <div class="detail-layout">
        <div>
          <section class="card">
            <h2 class="card__title">Description</h2>
            <div class="card__text">
              <p>{{ issue.description }}</p>
              <!-- Event Info: filled in when the issue came from a captured error -->
            </div>
          </section>

          <section class="card">
            <h2 class="card__title">Add a Note</h2>
            <div class="card__text"><!-- note form --></div>
          </section>
        </div>

        <aside>
          <section class="card">
            <h2 class="card__title">Details</h2>
            <div class="card__text"><!-- project, events, due, reported, repo, commit --></div>
          </section>

          <section class="card">
            <h2 class="card__title">Current Assignments</h2>
            <div class="card__text"><!-- one row per role held on this issue --></div>
          </section>

          <section class="card">
            <h2 class="card__title">Progress</h2>
            <div class="card__text"><!-- the five steps, current one marked --></div>
          </section>
        </aside>
      </div>
    </template>

    <p v-else>That issue could not be loaded.</p>
  </main>
</template>
