// router/index.ts
import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

// Import views
import Home from '@/views/Home.vue'
import Register from '@/views/Register.vue'
import Login from '@/views/Login.vue'
import Dashboard from '@/views/Dashboard.vue'
import Logout from '@/views/Logout.vue'
import Projects from '@/views/Projects.vue'
import ProjectPage from '@/views/ProjectPage.vue'
import CreateProject from '@/views/CreateProject.vue'
import UpdateProjectPage from '@/views/UpdateProjectPage.vue'
import Issues from '@/views/Issues.vue'
import Events from '@/views/Events.vue'
import Report from '@/views/Report.vue'
import Profile from '@/views/Profile.vue'
import Settings from '@/views/Settings.vue'
import Help from '@/views/Help.vue'
import Contact from '@/views/Contact.vue'
import About from '@/views/About.vue'
import Messages from '@/views/Messages.vue'
import Team from '@/views/Team.vue'
import Assignments from '@/views/Assignments.vue'
import Notifications from '@/views/Notifications.vue'
import Calendar from '@/views/Calendar.vue'

const routes: RouteRecordRaw[] = [
  // Default redirect (handled in beforeEach)
  {
    path: '/',
    name: 'Root',
    redirect: () => {
      // This redirect logic is handled in beforeEach guard
      return '/dashboard'
    }
  },

  // Public routes (only if NOT authenticated)
  {
    path: '/home',
    name: 'Home',
    component: Home,
    meta: { publicOnly: true }
  },
  {
    path: '/login',
    name: 'Login',
    component: Login,
    meta: { publicOnly: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: Register,
    meta: { publicOnly: true }
  },

  // Protected routes (only if authenticated)
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: Dashboard,
    meta: { requiresAuth: true }
  },
  {
    path: '/logout',
    name: 'Logout',
    component: Logout,
    meta: { requiresAuth: true }
  },
  {
    path: '/report',
    name: 'Report',
    component: Report,
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: Profile,
    meta: { requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: Settings,
    meta: { requiresAuth: true }
  },
  {
    path: '/projects',
    name: 'Projects',
    component: Projects,
    meta: { requiresAuth: true }
  },
  {
    path: '/project/:projectId',
    name: 'ProjectPage',
    component: ProjectPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/project/update/:projectId',
    name: 'UpdateProjectPage',
    component: UpdateProjectPage,
    meta: { requiresAuth: true }
  },
  {
    path: '/projects/create',
    name: 'CreateProject',
    component: CreateProject,
    meta: { requiresAuth: true }
  },
  {
    path: '/vulns',
    redirect: '/issues'
  },
  {
    path: '/issues',
    name: 'Issues',
    component: Issues,
    meta: { requiresAuth: true }
  },
  {
    path: '/events',
    name: 'Events',
    component: Events,
    meta: { requiresAuth: true }
  },
  {
    path: '/help',
    name: 'Help',
    component: Help,
    meta: { requiresAuth: true }
  },
  {
    path: '/contact',
    name: 'Contact',
    component: Contact,
    meta: { requiresAuth: true }
  },
  {
    path: '/about',
    name: 'About',
    component: About,
    meta: { requiresAuth: true }
  },
  {
    path: '/messages',
    name: 'Messages',
    component: Messages,
    meta: { requiresAuth: true }
  },
  {
    path: '/team',
    name: 'Team',
    component: Team,
    meta: { requiresAuth: true }
  },
  {
    path: '/project/:projectId/assignments',
    name: 'Assignments',
    component: Assignments,
    meta: { requiresAuth: true }
  },
  {
    path: '/notifications',
    name: 'Notifications',
    component: Notifications,
    meta: { requiresAuth: true }
  },
  {
    path: '/calendar',
    name: 'Calendar',
    component: Calendar,
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

// Global navigation guard (happens before each redirect)
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  // true - logged in
  // false - logged out
  // null - loading...

  // Initialize auth if needed (if loading somehow)
  if (authStore.isAuthenticated === null) {
    await authStore.initAuth()
  }

  // Handle root path redirect with settings
  if (to.path === '/') {

    // avoids loading/warning page
    if (authStore.isAuthenticated === null) {
      next() // go to the route anyway
      return
    }

    // Not authenticated → go to home
    if (!authStore.isAuthenticated) {
      next({ path: '/home', replace: true })
      return
    }
  }

  /// Preset settings can be added here

  // Protected routes
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    // Not authenticated → redirect to login
    next({ path: '/login', replace: true })
    return
  }


  // Public only routes (login, register, home) handler when authenticated
  if (to.meta.publicOnly && authStore.isAuthenticated) {
    // Already authenticated → redirect to dashboard
    next({ path: '/dashboard', replace: true })
    return
  }

  // Allow navigation
  next()
})
export default router
