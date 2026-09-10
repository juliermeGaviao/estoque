<script setup>
import { StateService } from '@/service/StateService'
import api from '@/util/api'
import { onlyDigits } from '@/util/util'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useToast } from 'primevue/usetoast'
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'
import Contact from './Contact.vue'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const states = ref([])

const companyForm = ref(null)
const companyFormValues = ref({ razaoSocial: '', nome: '', cnpj: '', fone: '', endereco: '', bairro: '', cep: '', cidade: '', uf: '' })

const companyFormValidator = zodResolver(
  z.object({
    razaoSocial: z.string().trim().min(1, { message: 'Razão Social é obrigatório.' }),
    nome: z.string().trim().min(1, { message: 'Nome de Fantasia é obrigatório.' }),
    cnpj: z.string().length(18, { message: 'CNPJ é obrigatório.' }),
    fone: z.string().min(1, { message: 'Fone é obrigatório.' }).transform((val) => val.replaceAll(/\D/g, '')).refine((val) => /^\d{10,11}$/.test(val), { message: 'O telefone deve conter DDD + (8 ou 9) dígitos.' }),
    endereco: z.string().trim().min(1, { message: 'Endereço é obrigatório.' }),
    bairro: z.string().trim().min(1, { message: 'Bairro é obrigatório.' }),
    cep: z.string().length(9, { message: 'CEP é obrigatório.' }),
    cidade: z.string().trim().min(1, { message: 'Cidade é obrigatório.' }),
    uf: z.string().length(2, { message: 'UF é obrigatório.' })
  })
)

const id = ref(route.query.id)

async function load() {
  try {
    const res = await api.get('/client', { params: { id: id.value } })

    if (companyForm.value) {
      companyForm.value.setValues({
        razaoSocial: res.data.razaoSocial,
        nome: res.data.nome,
        cnpj: res.data.cnpj,
        fone: res.data.fone,
        endereco: res.data.endereco,
        bairro: res.data.bairro,
        cep: res.data.cep,
        cidade: res.data.cidade,
        uf: res.data.uf
      })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Empresa Cliente', detail: 'Requisição de empresa cliente terminou com o erro: ' + error?.response?.data, life: 10000 })
  }
}

const save = async ({ valid, values }) => {
  if (!valid) return

  let params = { ... values }

  for (let field of ['cnpj', 'fone', 'cep']) {
    params[field] = onlyDigits(params[field])
  }

  for (let param in params) {
    if (typeof params[param] === 'string') {
      params[param] = params[param].trim()
    }
  }

  params['id'] = Number.parseInt(id.value)

  try {
    const response = await api.post('/client', params)

    if (response.status === 200) {
      id.value = response.data.id

      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Empresa Cliente atualizada com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Empresa Cliente', detail: 'Requisição de alteração de empresa cliente terminou com o erro: ' + error?.response?.data, life: 10000 })
  }
}

onMounted(() => {
  StateService.getStates().then(data => states.value = data)

  if (id.value) {
    load()
  }
})

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
  <ConfirmDialog :closable="false"></ConfirmDialog>
  <Card class="mb-4">
    <template #title>
      <div class="grid grid-cols-2">
        <h3>{{ id ? 'Editar' : 'Inserir' }} Empresa Cliente</h3>
        <div class="flex justify-end items-center">
          <Button icon="pi pi-replay" @click="router.push('/register/company')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
        </div>
      </div>
    </template>

    <template #content>
      <Form ref="companyForm" :resolver="companyFormValidator" :initialValues="companyFormValues" @submit="save" class="grid flex flex-column gap-2">
        <FormField v-slot="$field" name="razaoSocial">
          <FloatLabel variant="on">
            <InputText id="razaoSocial" maxlength="255" autocomplete="off" fluid/>
            <label for="razaoSocial">Razão Social</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <FormField v-slot="$field" name="nome">
          <FloatLabel variant="on">
            <InputText id="nome" maxlength="255" autocomplete="off" fluid/>
            <label for="nome">Nome de Fantasia</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <div class="grid grid-cols-2 gap-2">
          <FormField v-slot="$field" name="cnpj">
            <FloatLabel variant="on">
              <InputMask id="cnpj" v-model="$field.value" mask="99.999.999/9999-99" autocomplete="off" fluid/>
              <label for="cnpj">CNPJ</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>

          <FormField v-slot="$field" name="fone">
            <FloatLabel variant="on">
              <InputMask id="fone" mask="(99) 9999-9999?9" autocomplete="off" fluid/>
              <label for="fone">Fone</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>
        </div>

        <FormField v-slot="$field" name="endereco">
          <FloatLabel variant="on">
            <InputText id="endereco" maxlength="255" autocomplete="off" fluid/>
            <label for="endereco">Endereço</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <div class="grid grid-cols-12 gap-2">
          <div class="col-span-10">
            <FormField v-slot="$field" name="bairro">
              <FloatLabel variant="on">
                <InputText id="bairro" maxlength="100" autocomplete="off" fluid/>
                <label for="bairro">Bairro</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>

          <div class="col-span-2">
            <FormField v-slot="$field" name="cep">
              <FloatLabel variant="on">
                <InputMask id="cep" mask="99999-999" autocomplete="off" fluid/>
                <label for="cep">CEP</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>
        </div>

        <div class="grid grid-cols-12 gap-2">
          <div class="col-span-10">
            <FormField v-slot="$field" name="cidade">
              <FloatLabel variant="on">
                <InputText id="cidade" maxlength="100" autocomplete="off" fluid/>
                <label for="cidade">Cidade</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>

          <div class="col-span-2">
            <FormField v-slot="$field" name="uf">
              <FloatLabel variant="on">
                <Select id="uf" :options="states" optionLabel="name" optionValue="code" fluid/>
                <label for="uf">Estado</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>
        </div>

        <FormField class="flex justify-end gap-2">
          <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
          <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
        </FormField>
      </Form>
    </template>
  </Card>
  <Contact :id="id"/>
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
