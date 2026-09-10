<script setup>
import api from '@/util/api'
import { useToast } from 'primevue/usetoast'
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const props = defineProps({
  id: { type: [Number, String], default: null }
})

const router = useRouter()
const toast = useToast()

const id = ref(props.id)

const fileupload = ref({ files: []})

async function upload() {
  const file = fileupload.value.files[0]

  if (!file.name.endsWith('.csv')) {
    toast.add({ severity: 'error', summary: 'Tipo inválido', detail: 'Apenas arquivos CSV são permitidos.', life: 10000 })
    return
  }

  if (file.size > 10 * 1024 * 1024) {
    toast.add({ severity: 'error', summary: 'Arquivo muito grande', detail: 'O tamanho máximo é de 10MB.', life: 10000 })
    return
  }

  const formData = new FormData()
  formData.append('idEmpresa', id.value)
  formData.append('file', file)

  try {
    const response = await api.post('/client/load-employees', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })

    if (response.status === 200) {
      const data = response.data
      let message

      if (data.carregados > 1) {
        message = `${data.carregados} colaboradores carregados de ${data.total} enviados.`
      } else {
        message = `${data.carregados} colaborador carregado de ${data.total} enviados.`
      }

      toast.add({ severity: 'success', summary: 'Carga concluída', detail: message, life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Erro na carga', detail: error.message, life: 10000 })
  }
}

function clearUpload() {
  fileupload.value.clear()
}

const pop = ref()

function togglePopover(event) {
  pop.value.toggle(event)
}
</script>

<template>
  <Card v-show="id">
    <template #title>
      <div class="grid grid-cols-2">
        <h3>
          Carregar Colaboradores
          <i ref="infoIcon" class="pi pi-info-circle" @click="togglePopover" style="cursor: pointer; color: black;"/>
          <Popover ref="pop" style="max-width: 600px; transform: translateX(-12px)">
              <h4>Arquivo de Colaboradores</h4>
              <p>O arquivo de colaboradores da empresa é um arquivo texto onde cada linha contém dados do colaborador.
                Este arquivo não deve ultrapassar o tamanho de 10MB.
                Ele é do formato CSV, ou seja, as linhas representam campos separados por "," (vírgula).
              </p>
              <p>A primeira linha do arquivo é a linha de cabeçalho identificando os campos de dados do colaboradores e deve conter, obrigatoriamente, o seguinte conteúdo:</p>
              <ul style="list-style-type: disc; margin-left: 1.5rem;">
                <li>nome: contém o nome completo do colaborador;</li>
                <li>numero-cracha: valor alfanumérico que identifica o colaborador na empresa;</li>
                <li>data-aniversario: é a data de nascimento do colaborador no formato dd/mm/aaaa;</li>
                <li>limite-gasto: valor em R$ limite para desconto na folha de pagamento. Não é necessário informar os centavos;</li>
              </ul>
              <p>Assim, as demais linhas devem conter os dados dos colaboradores separados por vírgula e seguindo as regras enunciadas nos tópicos acima.</p>
              <p>Exemplo:</p>
              <p>nome, numero-cracha, data-aniversario, limite-gasto<br/>
                Fulano, DKJF-DC, 01/01/1970, 400<br/>
                Ciclano, 398943, 23/08/1983, 450<br/>
                Beltrano, DFKJFD, 09/03/2003, 600
              </p>
          </Popover>
        </h3>
        <div class="flex justify-end items-center">
          <Button icon="pi pi-replay" @click="router.push('/register/company')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
        </div>
      </div>
    </template>
    <template #content>
      <div class="flex justify-between">
        <FileUpload ref="fileupload" mode="basic" accept=".csv, text/csv" :maxFileSize="10*1024*1024"/>
        <div class="flex justify-end gap-2">
          <Button label="Limpar" @click="clearUpload" icon="pi pi-times" severity="secondary" raised/>
          <Button label="Carregar" @click="upload" severity="primary" raised :disabled="!fileupload?.files?.length"/>
        </div>
      </div>
    </template>
  </Card>
</template>
