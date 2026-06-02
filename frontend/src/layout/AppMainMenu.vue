<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { eAdmin } from '../util/auth'

const router = useRouter()
const menu = ref()

const items = computed(() => [
  { label: 'Início', icon: 'pi pi-home', command: () => router.push('/') },
  { label: 'Vendas', icon: 'pi pi-shopping-cart', command: () => router.push('/core/sale') },
  {
    label: 'Estoque',
    icon: 'pi pi-box',
    visible: eAdmin(),
    items: [
      { label: 'Pedido de Compra', icon: 'pi pi-file-edit', command: () => router.push('/register/provider') },
      { label: 'Transferência', icon: 'pi pi-truck', command: () => router.push('/register/provider') }
    ]
  },
  {
    label: 'Cadastro',
    icon: 'pi pi-id-card',
    visible: eAdmin(),
    items: [
      { label: 'Fornecedor', icon: 'pi pi-building-columns', command: () => router.push('/register/provider') },
      { label: 'Ponto de Venda', icon: 'pi pi-shop', command: () => router.push('/register/sale-point') },
      { separator: true }, 
      { label: 'Cliente Empresa', icon: 'pi pi-building', command: () => router.push('/register/company') },
      { label: 'Cliente Pessoa', icon: 'pi pi-users', command: () => router.push('/register/person') },
      { separator: true }, 
      { label: 'Tipo de Produto', icon: 'pi pi-tags', command: () => router.push('/register/product-type') },
      { label: 'Produto', icon: 'pi pi-tag', command: () => router.push('/register/product') },
      { label: 'Tabela de Preços', icon: 'pi pi-dollar', command: () => router.push('/register/price-table') }
    ]
  },
  { label: 'Indicadores', icon: 'pi pi-chart-bar', visible: eAdmin(), command: () => router.push('/dashboard') }
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