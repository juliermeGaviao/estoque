<script setup>
import TieredMenu from 'primevue/tieredmenu'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const menu = ref()

const items = ref([
  {
    label: 'Cadastro',
    icon: 'pi pi-id-card',
    items: [
      {
        label: 'Fornecedor',
        icon: 'pi pi-building-columns',
        command: () => { router.push('/register/provider') }
      },
      {
        label: 'Cliente Empresa',
        icon: 'pi pi-building',
        command: () => { router.push('/register/company') }
      },
      {
        label: 'Cliente Pessoa',
        icon: 'pi pi-users',
        command: () => { router.push('/register/person') }
      },
      {
        label: 'Tipo de Produto',
        icon: 'pi pi-tags',
        command: () => { router.push('/register/person') }
      },
      {
        label: 'Produto',
        icon: 'pi pi-box',
        command: () => { router.push('/register/person') }
      }
    ]
  },
  {
    label: 'Indicadores',
    icon: 'pi pi-chart-bar'
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
  <TieredMenu ref="menu" :model="items" popup/>
</template>