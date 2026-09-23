<script setup lang="ts">
// Dashboard: the counters, your issues split by the role you hold on each one,
// the event groups waiting to be triaged, and the projects you belong to.
const stats = [
  { label: 'Open Issues', value: 12 },
  { label: 'Patched', value: 5 },
  { label: 'Overdue', value: 2 },
  { label: 'Waiting on you', value: 3 },
]

const reported = [
  { key: 'CLM-131', title: 'Rate limit exceeded for client IP 198.51.100.22', status: 'Reported' },
  { key: 'CLM-134', title: 'Refresh token retry storm on expiry', status: 'In Progress' },
  { key: 'CLM-127', title: 'Due date picker accepts past dates', status: 'Reported' },
  { key: 'CLM-118', title: 'Calendar day numbers unreadable', status: 'Under Review' },
]

const assigned = [
  { key: 'CLM-139', title: 'NullPointerException in PaymentService', status: 'Patched' },
  { key: 'CLM-128', title: 'Team page loads member lists one at a time', status: 'In Progress' },
  { key: 'CLM-137', title: 'Session cookie not cleared after logout', status: 'Patched' },
]

const verified = [
  { key: 'CLM-119', title: 'Duplicate notification emails on role change', status: 'Verified' },
  { key: 'CLM-112', title: 'Stale project list after leaving a project', status: 'Verified' },
]

const eventGroups = [
  { name: 'RateLimitExceededException', count: 320 },
  { name: 'TimeoutException: billing provider', count: 12 },
  { name: 'ValidationError: dueAt in past', count: 7 },
]

const projects = [
  { name: 'api-gateway', open: 5, role: 'Lead' },
  { name: 'checkout-service', open: 4, role: 'Member' },
  { name: 'clamped-web', open: 3, role: 'Member' },
]

const statusColor = (s: string) =>
  ({ Reported: 'grey', 'In Progress': 'warning', Patched: 'success', 'Under Review': 'info', Verified: 'purple' }[s] ?? 'grey')

const lists = [
  { title: 'Recent Reports', items: reported, more: 'See all 8 reported' },
  { title: 'Assigned to You', items: assigned, more: 'See all 3 assigned' },
  { title: 'Verified by You', items: verified, more: 'See all 5 verified' },
]
</script>

<template>
  <v-container fluid class="pa-6">
    <!-- Title with the counters on the same line -->
    <div class="d-flex flex-wrap align-center justify-space-between mb-4">
      <h1 class="text-h5 font-weight-bold text-primary">Where You Left Off</h1>
      <div class="d-flex flex-wrap ga-4 text-body-2 text-medium-emphasis">
        <span v-for="s in stats" :key="s.label">{{ s.label }}: <strong>{{ s.value }}</strong></span>
      </div>
    </div>

    <div class="d-flex align-center justify-space-between mb-2">
      <h2 class="text-subtitle-1 font-weight-medium">My Issues</h2>
      <v-btn color="info" size="small" prepend-icon="mdi-plus" to="/report">Report Issue</v-btn>
    </div>

    <v-row>
      <v-col v-for="list in lists" :key="list.title" cols="12" md="4">
        <v-card variant="elevated" elevation="2" height="100%">
          <v-card-title class="text-subtitle-1">{{ list.title }}</v-card-title>
          <v-list density="compact" class="py-0">
            <v-list-item v-for="i in list.items" :key="i.key" :to="`/issues/${i.key}`">
              <v-list-item-title class="text-body-2 text-truncate">{{ i.title }}</v-list-item-title>
              <template #append>
                <v-chip :color="statusColor(i.status)" size="x-small" variant="tonal">{{ i.status }}</v-chip>
              </template>
            </v-list-item>
          </v-list>
          <v-card-actions class="pt-0">
            <v-btn variant="text" size="small" to="/issues">{{ list.more }}</v-btn>
          </v-card-actions>
        </v-card>
      </v-col>
    </v-row>

    <!-- Triage queue beside the project list, rather than another three cards -->
    <v-row class="mt-2">
      <v-col cols="12" md="6">
        <v-card variant="elevated" elevation="2">
          <v-card-title class="text-subtitle-1">Event Groups to Triage</v-card-title>
          <v-list density="compact" class="py-0">
            <v-list-item v-for="g in eventGroups" :key="g.name">
              <v-list-item-title class="text-body-2">
                <code>{{ g.name }}</code>
                <span class="text-medium-emphasis ml-2">{{ g.count }} events</span>
              </v-list-item-title>
              <template #append>
                <v-btn size="x-small" variant="outlined" to="/report">Promote</v-btn>
              </template>
            </v-list-item>
          </v-list>
        </v-card>
      </v-col>

      <v-col cols="12" md="6">
        <v-card variant="elevated" elevation="2">
          <v-card-title class="d-flex align-center justify-space-between text-subtitle-1">
            My Projects
            <v-btn color="info" size="x-small" prepend-icon="mdi-plus" to="/projects/create">Create Project</v-btn>
          </v-card-title>
          <v-list density="compact" class="py-0">
            <v-list-item v-for="p in projects" :key="p.name" :to="`/project/${p.name}`">
              <v-list-item-title class="text-body-2">
                {{ p.name }}
                <span class="text-medium-emphasis ml-2">{{ p.open }} open</span>
              </v-list-item-title>
              <template #append>
                <v-chip size="x-small" variant="tonal">{{ p.role }}</v-chip>
              </template>
            </v-list-item>
          </v-list>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>
