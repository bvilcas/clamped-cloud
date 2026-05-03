<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const features = [
  {
    icon: 'mdi-shield-alert-outline',
    title: 'Track Vulnerabilities',
    description: 'Log, triage, and resolve security issues with full severity tracking and lifecycle management.'
  },
  {
    icon: 'mdi-account-group-outline',
    title: 'Team Collaboration',
    description: 'Assign roles, coordinate fixes, and communicate directly inside each project channel.'
  },
  {
    icon: 'mdi-folder-check-outline',
    title: 'Project Management',
    description: 'Organize work across projects, manage members, and track progress from report to patch.'
  }
]

onMounted(async () => {
  await authStore.initAuth()
})

watch(
  () => authStore.isAuthenticated,
  (isAuthenticated) => {
    if (isAuthenticated === true) {
      router.replace('/dashboard')
    }
  },
  { immediate: true }
)
</script>

<template>
  <div class="landing-root">

    <!-- Top nav -->
    <header class="landing-nav">
      <img src="/logo.png" alt="Clamped" class="landing-logo" />
      <div class="d-flex ga-2 align-center">
        <v-btn variant="text" color="secondary" @click="router.push('/login')">Sign In</v-btn>
        <v-btn color="info" rounded="lg" @click="router.push('/register')">Get Started</v-btn>
      </div>
    </header>

    <!-- Hero -->
    <section class="hero-section">
      <div class="hero-content">
        <v-chip
          color="info"
          variant="tonal"
          size="small"
          prepend-icon="mdi-shield-lock-outline"
          class="mb-6"
        >
          Security Management Platform
        </v-chip>

        <h1 class="hero-headline text-info">
          Vulnerability Management,<br />Streamlined.
        </h1>

        <p class="hero-subtitle text-secondary">
          Clamped brings your security team together. Track vulnerabilities,
          manage projects, and coordinate fixes — all in one place.
        </p>

        <div class="d-flex ga-3 flex-wrap justify-center">
          <v-btn
            color="info"
            size="large"
            rounded="lg"
            append-icon="mdi-arrow-right"
            @click="router.push('/register')"
          >
            Get Started Free
          </v-btn>
          <v-btn
            size="large"
            variant="outlined"
            color="secondary"
            rounded="lg"
            @click="router.push('/login')"
          >
            Sign In
          </v-btn>
        </div>
      </div>

      <!-- Decorative stat chips -->
      <div class="d-flex justify-center ga-3 flex-wrap mt-12">
        <v-chip prepend-icon="mdi-check-circle-outline" color="success" variant="tonal">
          Role-based Access
        </v-chip>
        <v-chip prepend-icon="mdi-lock-outline" color="info" variant="tonal">
          Secure by Default
        </v-chip>
        <v-chip prepend-icon="mdi-lightning-bolt-outline" color="warning" variant="tonal">
          Real-time Messaging
        </v-chip>
      </div>
    </section>

    <!-- Feature cards -->
    <section class="features-section">
      <h2 class="features-heading text-center mb-2">Everything your security team needs</h2>
      <p class="text-center text-secondary mb-10">
        One platform to manage the full vulnerability lifecycle.
      </p>
      <v-container>
        <v-row justify="center">
          <v-col v-for="f in features" :key="f.title" cols="12" sm="6" md="4">
            <v-card variant="elevated" elevation="2" class="feature-card h-100">
              <v-card-text class="d-flex flex-column align-center text-center pa-8">
                <div class="feature-icon-wrap mb-5">
                  <v-icon :icon="f.icon" color="info" size="32" />
                </div>
                <h3 class="feature-title mb-2">{{ f.title }}</h3>
                <p class="text-secondary text-body-2">{{ f.description }}</p>
              </v-card-text>
            </v-card>
          </v-col>
        </v-row>
      </v-container>
    </section>

    <!-- Footer CTA -->
    <section class="cta-section">
      <div class="cta-box">
        <v-icon icon="mdi-shield-lock-outline" color="info" size="40" class="mb-4" />
        <h2 class="cta-heading mb-2">Ready to secure your projects?</h2>
        <p class="text-secondary mb-6">
          Join your team on Clamped and start managing vulnerabilities today.
        </p>
        <v-btn
          color="info"
          size="large"
          rounded="lg"
          append-icon="mdi-arrow-right"
          @click="router.push('/register')"
        >
          Create an Account
        </v-btn>
      </div>
    </section>

    <!-- Footer -->
    <footer class="landing-footer">
      <span class="text-secondary text-caption">© 2025 Clamped. All rights reserved.</span>
    </footer>

  </div>
</template>

<style scoped>
.landing-root {
  min-height: 100vh;
  background: rgb(var(--v-theme-background));
  display: flex;
  flex-direction: column;
}

/* Nav */
.landing-nav {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 48px;
  border-bottom: 1px solid rgb(var(--v-theme-outline));
  position: sticky;
  top: 0;
  background: rgb(var(--v-theme-background));
  z-index: 10;
}

.landing-logo {
  height: 38px;
  width: auto;
}

/* Hero */
.hero-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 100px 24px 80px;
}

.hero-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  max-width: 680px;
}

.hero-headline {
  font-size: 3rem;
  font-weight: 700;
  line-height: 1.2;
  margin-bottom: 20px;
}

.hero-subtitle {
  font-size: 1.1rem;
  line-height: 1.75;
  margin-bottom: 36px;
}

/* Features */
.features-section {
  padding: 80px 0;
  background: rgb(var(--v-theme-surface-variant));
}

.features-heading {
  font-size: 1.75rem;
  font-weight: 600;
  color: rgb(var(--v-theme-on-surface));
}

.feature-card {
  border-radius: 16px !important;
  transition: box-shadow 0.2s;
}

.feature-card:hover {
  box-shadow: 0 8px 24px rgba(37, 99, 235, 0.12) !important;
}

.feature-icon-wrap {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: rgba(37, 99, 235, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
}

.feature-title {
  font-size: 1rem;
  font-weight: 600;
  color: rgb(var(--v-theme-on-surface));
}

/* CTA section */
.cta-section {
  padding: 80px 24px;
  display: flex;
  justify-content: center;
}

.cta-box {
  max-width: 560px;
  text-align: center;
}

.cta-heading {
  font-size: 1.75rem;
  font-weight: 600;
  color: rgb(var(--v-theme-on-surface));
}

/* Footer */
.landing-footer {
  padding: 24px 48px;
  border-top: 1px solid rgb(var(--v-theme-outline));
  text-align: center;
}

@media (max-width: 600px) {
  .landing-nav {
    padding: 16px 20px;
  }
  .hero-headline {
    font-size: 2rem;
  }
  .hero-section {
    padding: 60px 20px 48px;
  }
}
</style>
