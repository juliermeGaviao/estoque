import { z } from 'zod'

export function createStockTransferSchema() {
  return z.object({
    idPontoVendaOrigem: z.number({ required_error: 'Ponto de Venda Origem é obrigatório.' }).nullable().refine((val) => val !== null && val !== undefined, { message: 'Ponto de Venda Origem é obrigatório.' }),
    idPontoVendaDestino: z.number({ required_error: 'Ponto de Venda Destino é obrigatório.' }).nullable().refine((val) => val !== null && val !== undefined, { message: 'Ponto de Venda Destino é obrigatório.' }),
    dataTransferencia: z.any().nullable().optional()
  })
}