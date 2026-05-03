<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()

const form = reactive({
  firstname: '',
  lastname: '',
  email: '',
  password: ''
})

const error = ref('')
const loading = ref(false)
const showPassword = ref(false)

const bullets = [
  { icon: 'mdi-shield-check-outline', text: 'Full vulnerability lifecycle tracking' },
  { icon: 'mdi-account-multiple-outline', text: 'Team roles and project management' },
  { icon: 'mdi-message-text-outline', text: 'Real-time project messaging' }
]

const handleSubmit = async () => {
  error.value = ''
  loading.value = true
  localStorage.removeItem('accessToken')

  try {
    const res = await fetch('/api/v1/auth/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify(form)
    })

    if (!res.ok) throw new Error('Registration failed')

    const data = await res.json()
    authStore.login(data.accessToken)
    router.replace('/dashboard')
  } catch (err) {
    console.error(err)
    error.value = 'Could not create account. Please check your details and try again.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-root">
    <!-- Left branding panel -->
    <div class="brand-panel">
      <div class="brand-inner">
        <img src="/logo.png" alt="Clamped" class="brand-logo" />
        <h2 class="brand-headline">Security starts with visibility.</h2>
        <p class="brand-sub">
          Clamped gives your team one place to track, triage, and resolve
          every vulnerability from report to patch.
        </p>
        <div class="brand-bullets">
          <div v-for="b in bullets" :key="b.text" class="brand-bullet">
            <v-icon :icon="b.icon" size="18" color="white" class="mr-2" />
            <span>{{ b.text }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Right form panel -->
    <div class="form-panel">
      <div class="form-inner">
        <!-- Back to home -->
        <v-btn
          variant="text"
          color="secondary"
          size="small"
          prepend-icon="mdi-arrow-left"
          class="mb-6 px-0"
          @click="router.push('/home')"
        >
          Back to home
        </v-btn>

        <h1 class="form-title text-info">Create your account</h1>
        <p class="text-secondary mb-6">Join your team on Clamped.</p>

        <v-form @submit.prevent="handleSubmit">
          <v-row dense class="mb-1">
            <v-col cols="6">
              <v-text-field
                v-model="form.firstname"
                label="First name"
                variant="outlined"
                density="comfortable"
                prepend-inner-icon="mdi-account-outline"
                autocomplete="given-name"
                required
              />
            </v-col>
            <v-col cols="6">
              <v-text-field
                v-model="form.lastname"
                label="Last name"
                variant="outlined"
                density="comfortable"
                autocomplete="family-name"
                required
              />
            </v-col>
          </v-row>

          <v-text-field
            v-model="form.email"
            label="Email address"
            type="email"
            variant="outlined"
            density="comfortable"
            prepend-inner-icon="mdi-email-outline"
            autocomplete="email"
            class="mb-3"
            required
          />

          <v-text-field
            v-model="form.password"
            label="Password"
            :type="showPassword ? 'text' : 'password'"
            variant="outlined"
            density="comfortable"
            prepend-inner-icon="mdi-lock-outline"
            :append-inner-icon="showPassword ? 'mdi-eye-off-outline' : 'mdi-eye-outline'"
            autocomplete="new-password"
            class="mb-1"
            required
            @click:append-inner="showPassword = !showPassword"
          />

          <v-alert
            v-if="error"
            type="error"
            variant="tonal"
            density="compact"
            class="mb-4"
            prepend-icon="mdi-alert-circle-outline"
          >
            {{ error }}
          </v-alert>

          <v-btn
            type="submit"
            color="info"
            block
            size="large"
            rounded="lg"
            :loading="loading"
            class="mt-2"
          >
            Create Account
          </v-btn>
        </v-form>

        <div class="text-center mt-6">
          <span class="text-secondary text-body-2">Already have an account? </span>
          <v-btn
            variant="text"
            color="info"
            size="small"
            class="pa-0 text-body-2"
            style="text-transform: none; min-width: unset;"
            @click="router.push('/login')"
          >
            Sign in
          </v-btn>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-root {
  min-height: 100vh;
  display: flex;
}

/* Left panel */
.brand-panel {
  width: 420px;
  flex-shrink: 0;
  background: rgb(var(--v-theme-info));
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
}

.brand-inner {
  max-width: 320px;
}

.brand-logo {
  height: 40px;
  width: auto;
  filter: brightness(0) invert(1);
  margin-bottom: 40px;
}

.brand-headline {
  font-size: 1.6rem;
  font-weight: 700;
  color: #ffffff;
  line-height: 1.3;
  margin-bottom: 12px;
}

.brand-sub {
  font-size: 0.95rem;
  color: rgba(255, 255, 255, 0.8);
  line-height: 1.65;
  margin-bottom: 32px;
}

.brand-bullets {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.brand-bullet {
  display: flex;
  align-items: center;
  color: rgba(255, 255, 255, 0.9);
  font-size: 0.9rem;
}

/* Right panel */
.form-panel {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 32px;
  background: rgb(var(--v-theme-background));
}

.form-inner {
  width: 100%;
  max-width: 420px;
}

.form-title {
  font-size: 1.75rem;
  font-weight: 700;
  margin-bottom: 4px;
}

@media (max-width: 768px) {
  .brand-panel {
    display: none;
  }
}
</style>
