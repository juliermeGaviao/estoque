import { z } from 'zod'

export function createPurchaseOrderSchema(getAction) {
  return z.object({
    numeroPedido: z.string().trim().min(1).nullish().refine(val => val && val.trim().length > 0, { message: 'Número do Pedido é obrigatório.' }),
    idFornecedor: z.number({ required_error: 'Fornecedor é obrigatório.' }),
    dataPedido: z.any().refine(val => getAction() === 'visualizar' || val, { message: 'Data do Pedido é obrigatória.' })
  })
}