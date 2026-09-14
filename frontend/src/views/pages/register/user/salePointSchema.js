import { z } from 'zod'

export function createSalePointSchema(getUserProfiles) {
  return z.object({
    pontos: z.array(z.number()).refine(data => getUserProfiles() === 1 || data.length, { message: 'É necessário marcar ao menos um ponto de vendas.' }),
    ponto: z.number().refine(data => getUserProfiles() === 2 || data > 0, { message: 'Um Ponto de Venda deve ser escolhida.' })
  })
}