import { ref } from 'vue'

const visible = ref(true)

export function useSidebar() {
  function toggle() {
    visible.value = !visible.value
  }
  return { sidebarVisible: visible, toggleSidebar: toggle }
}
