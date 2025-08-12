<script setup>
import { logout } from '@/util/auth'
import TieredMenu from 'primevue/tieredmenu'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const menu = ref()

const items = ref([
  {
    label: 'Minha Conta',
    icon: 'pi pi-cog',
    command: () => { router.push('/') }
  },
  {
    label: 'Sair',
    icon: 'pi pi-sign-out',
    command: () => {
      logout()
      router.push('/auth/login')
    }
  }
])

const toggleMenu = (event) => {
  if (menu.value) {
    menu.value.toggle(event)
  }
}
</script>

<template>
  <button class="layout-menu-button layout-topbar-action" @click="toggleMenu($event)">
    <i class="pi pi-user"></i>
  </button>
  <TieredMenu ref="menu" :model="items" popup/>
</template>