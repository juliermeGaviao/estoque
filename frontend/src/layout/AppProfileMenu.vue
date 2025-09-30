<script setup>
import TieredMenu from 'primevue/tieredmenu'
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getUserId, logout, temPerfil } from '../util/auth'

const router = useRouter()
const menu = ref()

const items = computed(() => [
  {
    label: 'Minha Conta',
    icon: 'pi pi-cog',
    command: () => { router.push(`/register/user/edit?id=${getUserId()}`) }
  },
  temPerfil('admin') && {
    label: 'Gestão de Usuários',
    icon: 'pi pi-users',
    command: () => { router.push('/register/user') }
  },
  {
    label: 'Sair',
    icon: 'pi pi-sign-out',
    command: () => {
      logout()
      router.push('/auth/login')
    }
  }
].filter(Boolean))

function toggleMenu(event) {
  menu.value.toggle(event)
}
</script>

<template>
  <button class="layout-menu-button layout-topbar-action" @click="toggleMenu">
    <i class="pi pi-user"></i>
  </button>
  <TieredMenu ref="menu" :model="items" popup/>
</template>