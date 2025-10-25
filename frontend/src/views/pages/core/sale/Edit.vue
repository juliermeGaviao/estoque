<script setup>
import SaleComponent from '@/components/SaleComponent.vue';
import { useRoute } from 'vue-router';

const route = useRoute()
</script>

<template>
  <SaleComponent :id="route.query.id"/>
</template>
