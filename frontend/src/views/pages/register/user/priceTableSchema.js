import { z } from 'zod'

export function createPriceTableSchema(getUserProfiles) {
  return z.object({
    tabelas: z.array(z.number()).refine(data => getUserProfiles() === 1 || data.length, { message: 'É necessário marcar ao menos uma tabela de preços.' }),
    tabela: z.number().refine(data => getUserProfiles() === 2 || data > 0, { message: 'Uma Tabela de Preços deve ser escolhida.' })
  })
}
