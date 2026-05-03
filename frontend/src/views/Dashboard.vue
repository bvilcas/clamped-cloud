<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { fetchWithAuth } from '@/utils/fetchWithAuth'
import type { UserDTO } from '@/dto/user-dto'
import type { ProjectDTO } from '@/dto/project-dto'
import type { IssueDTO } from '@/dto/issue-dto'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
// router
const router = useRouter()

// Get the store instance (you can now access the global state)
const authStore = useAuthStore()

// State variables
// refs are generic types, so we provide the type in <>
const user = ref<UserDTO | null>(null)
const reported = ref<IssueDTO[]>([])
const assigned = ref<IssueDTO[]>([])
const verified = ref<IssueDTO[]>([])

const projects = ref<ProjectDTO[]>([])


  onMounted(() => {
    // Data fetching =========

    // No need to type annotate since this is technically same concept as "const x = 5"
    // We use arrow functions to order each call using ".then()" later
    const fetchProfile = async () => {
      try {
        console.log("Sending token:", localStorage.getItem("accessToken"))

        const res = await fetchWithAuth("/api/v1/users/profile")
        if (res.ok) {
          const data = await res.json()
          console.log("🔎 /profile response:", data)
          user.value = data.data
        }
      } catch (err: unknown) {
        if (err instanceof Error) {
          console.error("Profile fetch failed:", err.message);
        } else {
          console.error("Profile fetch failed with unknown error");
        }
        authStore.logout(); // force logout if token refresh failed
      }
    };


    const fetchUserVulns = async () => {
      const base = "/api/v1/userissues";

      try {
        const [reportedRes, assignedRes, verifiedRes] = await Promise.all([
          fetchWithAuth(`${base}/reported`),
          fetchWithAuth(`${base}/assigned`),
          fetchWithAuth(`${base}/verified`),
        ]);

        if (reportedRes.ok) {
          reported.value = (await reportedRes.json()).data || [];
        }
        if (assignedRes.ok) {
          assigned.value = (await assignedRes.json()).data || [];
        }
        if (verifiedRes.ok) {
          verified.value = (await verifiedRes.json()).data || [];
        }

      } catch (err: unknown) {
        if (err instanceof Error) {
          console.error(" Vulnerabilities fetch failed:", err.message);
        } else {
          console.error(" Vulnerabilities fetch failed with unknown error");
        }
        authStore.logout(); // force logout if refresh failed mid-fetch
      }
    };

    const fetchProjects = async () => {
      const res = await fetchWithAuth("/api/v1/userprojects/all");
      const data = await res.json();
      projects.value = data.data || [];
    };

    // we defined functions above now we call them in order
    fetchProfile().then(() => {
      fetchUserVulns();
      fetchProjects();
    });
  });

  // Watch for authentication changes and redirect if logged out
  // Store result of first getter (watching the ref)
  watch(
    () => authStore.isAuthenticated, // Watch this
    (isAuth) => { // Receive the value
      // Logic
      if (isAuth === false) {
        router.replace('/login') // Prevents back button redirect
      }
    }
  )
</script>

<template>
  <v-container class="pa-8">
    <!-- Title row -->
    <div class="title-row">
      <h1 class="text-info">Where You Left Off</h1>
      <div class="stats-bar text-secondary text-body-1">
        <span>Open Vulns: <strong>{{ reported.length }}</strong></span>
        <span>Patched: <strong>{{ verified.length }}</strong></span>
        <span>Overdue: <strong>2</strong></span>
      </div>
    </div>

    <!-- First row: vuln cards -->
    <div class="section-header">
      <h2>My Issues</h2>
      <v-btn color="info" @click="router.push('/report')">+ Report Issue</v-btn>
    </div>
    <v-row class="mt-2">
      <v-col cols="12" sm="6" md="4">
        <v-card variant="elevated" elevation="2" class="h-100">
          <v-card-title>Recent Reports</v-card-title>
          <v-card-text>
            <p v-if="reported.length === 0" class="empty-state">No reports submitted.</p>
            <v-list v-else density="compact">
              <template v-for="(v, i) in reported" :key="v.id">
                <v-divider v-if="i > 0" />
                <v-list-item>{{ v.title }} - {{ v.status }}</v-list-item>
              </template>
            </v-list>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="4">
        <v-card variant="elevated" elevation="2" class="h-100">
          <v-card-title>Assigned to You</v-card-title>
          <v-card-text>
            <p v-if="assigned.length === 0" class="empty-state">No tasks assigned.</p>
            <v-list v-else density="compact">
              <template v-for="(v, i) in assigned" :key="v.id">
                <v-divider v-if="i > 0" />
                <v-list-item>{{ v.title }} - {{ v.status }}</v-list-item>
              </template>
            </v-list>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="4">
        <v-card variant="elevated" elevation="2" class="h-100">
          <v-card-title>Verified by You</v-card-title>
          <v-card-text>
            <p v-if="verified.length === 0" class="empty-state">No verifications yet.</p>
            <v-list v-else density="compact">
              <template v-for="(v, i) in verified" :key="v.id">
                <v-divider v-if="i > 0" />
                <v-list-item>{{ v.title }} - {{ v.status }}</v-list-item>
              </template>
            </v-list>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Second row: project cards -->
    <div class="section-header">
      <h2>My Projects</h2>
      <v-btn color="info" @click="router.push('/projects/create')">+ Create Project</v-btn>
    </div>
    <v-row class="mt-2">
      <v-col cols="12" sm="6" md="4">
        <v-card variant="elevated" elevation="2" class="h-100">
          <v-card-title>Recently Updated Projects</v-card-title>
          <v-card-text>
            <p v-if="projects.length === 0" class="empty-state">No recent project activity.</p>
            <v-list v-else density="compact">
              <template v-for="(p, i) in projects.slice(0, 5)" :key="p.id">
                <v-divider v-if="i > 0" />
                <v-list-item>{{ p.name }}</v-list-item>
              </template>
            </v-list>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="4">
        <v-card variant="elevated" elevation="2" class="h-100">
          <v-card-title>Your Activity Breakdown</v-card-title>
          <v-card-text>
            <v-list density="compact">
              <v-list-item>Reported: {{ reported.length }}</v-list-item>
              <v-divider />
              <v-list-item>Assigned: {{ assigned.length }}</v-list-item>
              <v-divider />
              <v-list-item>Verified: {{ verified.length }}</v-list-item>
            </v-list>
          </v-card-text>
        </v-card>
      </v-col>

      <v-col cols="12" sm="6" md="4">
        <v-card variant="elevated" elevation="2" class="h-100">
          <v-card-title>My Projects</v-card-title>
          <v-card-text>
            <p v-if="projects.length === 0" class="empty-state">No projects yet.</p>
            <v-list v-else density="compact">
              <template v-for="(p, i) in projects" :key="p.id">
                <v-divider v-if="i > 0" />
                <v-list-item style="cursor: pointer" @click="router.push(`/project/${p.id}`)">
                  {{ p.name }}
                </v-list-item>
              </template>
            </v-list>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>

    <!-- Admin panel (NOT SHOWN) -->
    <v-card v-if="user?.role === 'ADMIN'" variant="elevated" elevation="2" class="mt-6">
      <v-card-title>👑 Admin Panel</v-card-title>
      <v-card-text>
        <div class="d-flex ga-3">
          <v-btn color="info" @click="router.push('/admin/users')">Manage Users</v-btn>
          <v-btn color="secondary" @click="router.push('/admin/assign')">Assign Roles</v-btn>
        </div>
      </v-card-text>
    </v-card>

  </v-container>
</template>

<style scoped>
.title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 1.5rem;
}

.stats-bar {
  display: flex;
  gap: 1rem;
}

.empty-state {
  color: rgb(var(--v-theme-secondary));
  font-size: 0.875rem;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 0.5rem;
  margin-top: 1.5rem;
}
</style>
