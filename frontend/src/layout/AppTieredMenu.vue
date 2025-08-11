<script setup>
import TieredMenu from 'primevue/tieredmenu'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const menu = ref()

const items = ref([
  {
    label: 'Home',
    icon: 'pi pi-home',
    command: () => { router.push('/') }
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
    <i class="pi pi-bars"></i>
  </button>
  <TieredMenu ref="menu" id="overlay_tmenu" :model="items" popup/>
</template>