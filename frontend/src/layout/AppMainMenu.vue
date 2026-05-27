<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { eAdmin } from '../util/auth'

const router = useRouter()
const menu = ref()

const items = computed(() => [
  { label: 'Início', icon: 'pi pi-home', command: () => router.push('/') },
  { label: 'Vendas', icon: 'pi pi-shopping-cart', command: () => router.push('/core/sale') },
  eAdmin() && {
    label: 'Cadastro',
    icon: 'pi pi-id-card',
    items: [
      { label: 'Fornecedor', icon: 'pi pi-building-columns', command: () => router.push('/register/provider') },
      { label: 'Cliente Empresa', icon: 'pi pi-building', command: () => router.push('/register/company') },
      { label: 'Cliente Pessoa', icon: 'pi pi-users', command: () => router.push('/register/person') },
      { label: 'Tipo de Produto', icon: 'pi pi-tags', command: () => router.push('/register/product-type') },
      { label: 'Produto', icon: 'pi pi-box', command: () => router.push('/register/product') },
      { label: 'Tabela de Preços', icon: 'pi pi-dollar', command: () => router.push('/register/price-table') }
    ]
  },
  eAdmin() && { label: 'Indicadores', icon: 'pi pi-chart-bar', command: () => router.push('/dashboard') }
].filter(Boolean))

const toggleMenu = (event) => {
  if (menu.value) {
    menu.value.toggle(event)
  }
}
</script>

<template>
  <Menubar :model="items"/>
</template>