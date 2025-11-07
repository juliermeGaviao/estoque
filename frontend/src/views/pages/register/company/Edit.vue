<script setup>
import { StateService } from '@/service/StateService'
import api from '@/util/api'
import { formatPhone, onlyDigits } from '@/util/util'
import { zodResolver } from '@primevue/forms/resolvers/zod'
import { useConfirm } from "primevue/useconfirm"
import { useToast } from 'primevue/usetoast'
import { nextTick, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { z } from 'zod'

const router = useRouter()
const route = useRoute()
const toast = useToast()
const states = ref([])
const loading = ref(false)
const confirm = useConfirm()

const companyForm = ref(null)
const companyFormValues = ref({ razaoSocial: '', nome: '', cnpj: '', fone: '', endereco: '', bairro: '', cep: '', cidade: '', uf: '' })

const companyFormValidator = zodResolver(
  z.object({
    razaoSocial: z.string().min(1, { message: 'Razão Social é obrigatório.' }),
    nome: z.string().min(1, { message: 'Nome de Fantasia é obrigatório.' }),
    cnpj: z.string().length(18, { message: 'CNPJ é obrigatório.' }),
    fone: z.string().min(1, { message: 'Fone é obrigatório.' }).transform((val) => val.replaceAll(/\D/g, '')).refine((val) => val.length === 10 || val.length === 11, { message: 'O telefone deve conter DDD + (8 ou 9) dígitos).' }),
    endereco: z.string().min(1, { message: 'Endereço é obrigatório.' }),
    bairro: z.string().min(1, { message: 'Bairro é obrigatório.' }),
    cep: z.string().length(9, { message: 'CEP é obrigatório.' }),
    cidade: z.string().min(1, { message: 'Cidade é obrigatório.' }),
    uf: z.string().length(2, { message: 'UF é obrigatório.' })
  })
)

const id = ref(route.query.id)

const data = ref([])
const totalRecords = ref(0)
const page = ref(0)
const size = ref(15)
const sortField = ref(null)
const sortOrder = ref(null)

const contactForm = ref(null)
const contactFormValues = ref({ nome: '', cargo: '', fone: '', ramal: '', celular: '', email: '', dataAniversario: '', observacoes: '' })

const contactFormValidator = zodResolver(
  z.object({
    nome: z.string().min(1, { message: 'Nome do Contato é obrigatório.' }),
    cargo: z.string().min(1, { message: 'Cargo é obrigatório.' }),
    fone: z.string().optional(),
    ramal: z.string().optional(),
    celular: z.string().optional(),
    email: z.string().min(1, { message: 'E-mail é obrigatório.' }).email({ message: 'E-mail inválido.' }),
    dataAniversario: z.date().optional(),
    observacoes: z.string().optional()
  })
)

let idContact
const visible = ref(false)

async function load() {
  loading.value = true

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
    toast.add({ severity: 'error', summary: 'Falha de Carga de Empresa Cliente', detail: 'Requisição de empresa cliente terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }

  loadContacts()
}

async function loadContacts() {
  loading.value = true

  try {
    let params = {
      idEmpresa: id.value,
      page: page.value,
      size: size.value
    }

    if (sortField.value) {
      params.sort = sortField.value
  
      if (sortOrder) {
        params.sort += sortOrder.value === 1 ? ',asc' : ',desc'
      }
    }

    const response = await api.get('/company-client-contact/list', { params: params })

    data.value = response.data.content
    totalRecords.value = response.data.totalElements
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Carga de Contatos da Empresa Cliente', detail: 'Requisição de lista de contatos da empresa cliente terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

const save = async ({ valid, values }) => {
  if (!valid) return

  loading.value = true

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
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Empresa Cliente', detail: 'Requisição de alteração de empresa cliente terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    loading.value = false
  }
}

function onPage(event) {
  page.value = event.page
  size.value = event.rows
  loadContacts()
}

function onSort(event) {
  page.value = 0
  sortField.value = event.sortField
  sortOrder.value = event.sortOrder

  loadContacts()
}

function edit(contact) {
  visible.value = true

  nextTick(() => {
    if (contact) {
      idContact = contact.id

      contactForm.value.setValues({
        nome: contact.nome,
        cargo: contact.cargo,
        fone: contact.fone,
        ramal: contact.ramal,
        celular: contact.celular,
        email: contact.email,
        dataAniversario: new Date(contact.dataAniversario),
        observacoes: contact.observacoes
      })
    } else {
      contactForm.value.setValues(contactFormValues.value)
    }
  })
}

const saveContact = async ({ valid, values }) => {
  if (!valid) return

  loading.value = true

  let params = { ... values }

  params['cliente'] = { "id": id.value }

  if (idContact) {
    params['id'] = idContact
  }

  for (let field of ['fone', 'celular']) {
    params[field] = onlyDigits(params[field])
  }

  for (let param in params) {
    if (typeof params[param] === 'string') {
      params[param] = params[param].trim()
    }
  }

  try {
    const response = await api.post('/company-client-contact', params)

    if (response.status === 200) {
      toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Contato de Empresa Cliente atualizado com sucesso', life: 10000 })
    }
  } catch (error) {
    toast.add({ severity: 'error', summary: 'Falha de Gravação de Contato de Empresa Cliente', detail: 'Requisição de alteração de empresa cliente de fornecedor terminou com o erro: ' + error.response.data, life: 10000 })
  } finally {
    visible.value = false
    loading.value = false
    loadContacts()
  }
}

const confirmDelete = entity => {
  confirm.require({
    message: 'Deseja remover o contato?',
    header: "Alerta",
    icon: 'pi pi-info-circle',
    rejectProps: {
      label: 'Cancelar',
      severity: 'secondary',
      raised: true
    },
    acceptProps: {
      label: 'Remover',
      severity: 'danger',
      raised: true
    },
    accept: async () => {
      loading.value = true

      try {
        await api.delete(`/company-client-contact?id=${entity.id}`)

        toast.add({ severity: 'success', summary: 'Sucesso', detail: 'Contato de Empresa Cliente removido com sucesso', life: 10000 })
      } catch (error) {
        toast.add({ severity: 'error', summary: 'Falha de Remoção de Contato de Empresa Cliente', detail: 'Requisição de remoção de contato de empresa cliente terminou com o erro: ' + error.response.data, life: 10000 })
      } finally {
        loading.value = false
        loadContacts()
      }
    }
  })
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
  <BlockUI :blocked="loading" fullScreen>
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
    <Card class="mb-4">
      <template #title>
        <div class="grid grid-cols-2">
          <h3>Lista de Contatos da Empresa Cliente</h3>
          <div class="flex justify-end items-center">
            <Button icon="pi pi-replay" @click="router.push('/register/company')" class="p-button-text" v-tooltip.bottom="'Voltar'"/>
          </div>
        </div>
      </template>
      <template #content>
        <DataTable :value="data" :lazy="true" :paginator="true" :rows="size" :totalRecords="totalRecords"
          :first="page * size" @page="onPage" @sort="onSort" :sortField="sortField" :sortOrder="sortOrder" responsiveLayout="scroll" stripedRows
          :rowsPerPageOptions="[15, 30, 60, 100]" size="small">

          <Column field="id" header="Id" sortable/>
          <Column field="nome" header="Nome" sortable/>
          <Column field="cargo" header="Cargo" sortable/>
          <Column field="celular" header="Celular">
            <template #body="slotProps">
              {{ formatPhone(slotProps.data.celular) }}
            </template>
          </Column>

          <Column headerClass="flex justify-center" bodyClass="flex justify-center">
            <template #header>
              <Button icon="pi pi-plus" class="p-button-sm p-button-text p-mr-2" @click="edit(null)" :disabled="!id" v-tooltip.bottom="'Novo Contato'"/>
            </template>

            <template #body="slotProps">
              <Button icon="pi pi-pencil" class="p-button-sm p-button-text p-mr-2" @click="edit(slotProps.data)" v-tooltip.bottom="'Editar'"/>
              <Button icon="pi pi-trash" class="p-button-sm p-button-text p-button-danger" @click="confirmDelete(slotProps.data)" v-tooltip.bottom="'Remover'"/>
            </template>
          </Column>
        </DataTable>
      </template>
    </Card>
    <Card v-show="id">
      <template #title>
        <div class="grid grid-cols-2">
          <h3>
            Carregar Colaboradores
            <i ref="infoIcon" class="pi pi-info-circle" @click="togglePopover" style="cursor: pointer; color: black;"/>
            <Popover ref="pop" style="max-width: 600px;">
                <h4>Arquivo de Colaboradores</h4>
                <p>O arquivo de colaboradores da empresa é um arquivo texto onde cada linha contém dados do colaborador.
                  Este arquivo não deve ultrapassar o tamanho de 10MB.
                  Ele é do formato CSV, ou seja, as linhas representam campos separados por "," (vírgula).
                </p>
                <p>A primeira linha do arquivo é a linha de cabeçalho identificando os campos de dados do colaboradores e deve conter, obrigatoriamente, o seguinte conteúdo:</p>
                <p>
                  <ul style="list-style-type: disc; margin-left: 1.5rem;">
                    <li>nome: contém o nome completo do colaborador;</li>
                    <li>numero-cracha: valor alfanumérico que identifica o colaborador na empresa;</li>
                    <li>data-aniversario: é a data de nascimento do colaborador no formato dd/mm/aaaa;</li>
                    <li>limite-gasto: valor em R$ limite para desconto na folha de pagamento. Não é necessário informar os centavos;</li>
                  </ul>
                </p>
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
            <Button label="Carregar" @click="upload" severity="primary" raised :disabled="!fileupload.files.length"/>
          </div>
        </div>
      </template>
    </Card>
    <Dialog v-model:visible="visible" modal :closable="false" :header="idContact ? 'Editar Contato' : 'Inserir Contato'" style="width: 40%">
      <Form ref="contactForm" :resolver="contactFormValidator" :initialValues="contactFormValues" @submit="saveContact" class="grid flex flex-column gap-2">
        <FormField v-slot="$field" name="nome" class="mt-1">
          <FloatLabel variant="on">
            <InputText id="nomeDialog" maxlength="255" autocomplete="off" fluid/>
            <label for="nomeDialog">Nome</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <FormField v-slot="$field" name="cargo">
          <FloatLabel variant="on">
            <InputText id="cargo" maxlength="255" autocomplete="off" fluid/>
            <label for="cargo">Cargo</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <div class="grid grid-cols-3 gap-2">
          <FormField v-slot="$field" name="fone">
            <FloatLabel variant="on">
              <InputMask id="foneDialog" mask="(99) 9999-99999" autocomplete="off" fluid/>
              <label for="foneDialog">Fone</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>

          <FormField v-slot="$field" name="ramal">
            <FloatLabel variant="on">
              <InputText id="ramal" maxlength="20" autocomplete="off" fluid/>
              <label for="ramal">Ramal</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>

          <FormField v-slot="$field" name="celular">
            <FloatLabel variant="on">
              <InputMask id="celular" mask="(99) 99999-9999" autocomplete="off" fluid/>
              <label for="celular">Celular</label>
            </FloatLabel>
            <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
          </FormField>
        </div>

        <div class="grid grid-cols-12 gap-2">
          <div class="col-span-8">
            <FormField v-slot="$field" name="email" initialValue="">
              <FloatLabel variant="on" class="flex-1">
                <InputText id="email" maxlength="100" autocomplete="off" fluid/>
                <label for="email">E-mail</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>

          <div class="col-span-4">
            <FormField v-slot="$field" name="dataAniversario" initialValue="">
              <FloatLabel variant="on" class="flex-1">
                <DatePicker dateFormat="dd/mm/yy" fluid/>
                <label for="dataAniversario">Data de Aniversário</label>
              </FloatLabel>
              <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
            </FormField>
          </div>
        </div>

        <FormField v-slot="$field" name="observacoes" initialValue="">
          <FloatLabel variant="on" class="flex-1">
            <Textarea id="observacoes" rows="3" size="1024" style="resize: none" fluid/>
            <label for="email">Observações</label>
          </FloatLabel>
          <Message v-if="$field?.invalid" size="small" severity="error" variant="simple">{{ $field.error?.message }}</Message>
        </FormField>

        <FormField class="flex justify-end gap-2">
          <Button label="Limpar" icon="pi pi-times" type="reset" severity="secondary" raised/>
          <Button label="Cancelar" icon="pi pi-ban" @click="visible = false" severity="secondary" raised/>
          <Button label="Salvar" icon="pi pi-save" type="submit" raised/>
        </FormField>
      </Form>
    </Dialog>
  </BlockUI>
</template>
