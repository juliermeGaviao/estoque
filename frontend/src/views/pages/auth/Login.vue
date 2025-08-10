<script setup>
import FloatingConfigurator from '@/components/FloatingConfigurator.vue'
import { useToast } from 'primevue/usetoast'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import api from '../../../util/api'
import { login, sha256Hex } from '../../../util/auth'
const toast = useToast()

const email = ref('')
const password = ref('')
const checked = ref(false)
const router = useRouter()

const doLogin = async () => {
  if (!email.value || !password.value) {
    toast.add({ severity: 'error', summary: 'Falha de Autenticação', detail: 'Usuário e senha são obrigatórios', life: 5000 })
    return
  }

  const hashedPassword = await sha256Hex(password.value)

  try {
    const response = await api.post('/auth/login', {
      email: email.value,
      senha: hashedPassword
    })
  
    login(response.data, checked.value)
  
    router.push('/')
  } catch (error) {
    if (error.status === 403) {
      toast.add({ severity: 'error', summary: 'Falha de Autenticação', detail: 'Usuário ou senha inválidos', life: 5000 })
    } else {
      toast.add({ severity: 'error', summary: 'Falha de Autenticação', detail: 'Falha de comunicação. Tente mais tarde', life: 5000 })
    }
  }
}

</script>

<template>
  <FloatingConfigurator/>
  <Toast position="top-center"/>
  <div class="bg-surface-50 dark:bg-surface-950 flex items-center justify-center min-h-screen min-w-[100vw] overflow-hidden">
    <div class="flex flex-col items-center justify-center">
      <div style="border-radius: 56px; padding: 0.3rem; background: linear-gradient(180deg, var(--primary-color) 10%, rgba(33, 150, 243, 0) 30%)">
        <div class="w-full bg-surface-0 dark:bg-surface-900 py-20 px-8 sm:px-20" style="border-radius: 53px">
          <div class="mb-8 flex flex-col items-center justify-center">
            <Image src="/logo.png"/>
            <div class="text-surface-900 dark:text-surface-0 text-3xl font-medium mb-4">Benvindo ao Estoque!</div>
            <span class="text-muted-color font-medium">Informe as credenciais para continuar</span>
          </div>

          <div>
            <label for="email" class="block text-surface-900 dark:text-surface-0 text-xl font-medium mb-2">E-mail</label>
            <InputText id="email" type="text" placeholder="Endereço de E-mail" class="w-full md:w-[30rem] mb-8" v-model="email" />

            <label for="password" class="block text-surface-900 dark:text-surface-0 font-medium text-xl mb-2">Senha</label>
            <Password id="password" v-model="password" placeholder="Senha" :toggleMask="true" class="mb-4" fluid :feedback="false"></Password>

            <div class="flex items-center justify-between mt-2 mb-8 gap-8">
                <div class="flex items-center">
                    <Checkbox v-model="checked" id="rememberme1" binary class="mr-2"></Checkbox>
                    <label for="rememberme1">Lembrar de mim</label>
                </div>
                <span class="font-medium no-underline ml-2 text-right cursor-pointer text-primary">Esqueceu a senha?</span>
            </div>
            <Button label="Entrar" class="w-full" @click="doLogin"></Button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.pi-eye {
    transform: scale(1.6);
    margin-right: 1rem;
}

.pi-eye-slash {
    transform: scale(1.6);
    margin-right: 1rem;
}
</style>
