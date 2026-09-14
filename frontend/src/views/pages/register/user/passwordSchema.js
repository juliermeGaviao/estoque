import { z } from 'zod'

export const passwordSchema = z.object({
  senha: z.string().trim().min(1, { message: 'Senha é obrigatória.' }).min(8, { message: 'Senha deve ter no mínimo 8 caracteres.' }),
  confirmarSenha: z.string().trim().min(1, { message: 'Confirmação de senha é obrigatória.' })
}).refine(data => data.senha === data.confirmarSenha, {
  message: 'As senhas não coincidem.',
  path: ['confirmarSenha']
})
