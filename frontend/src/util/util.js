export function formatCpfCnpj(value) {
    if (!value) return ''

    const digits = value.replace(/\D/g, '')

    if (digits.length === 11) {
      return digits.replace(/^(\d{3})(\d{3})(\d{3})(\d{2})$/, '$1.$2.$3-$4')
    } else if (digits.length === 14) {
      return digits.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5')
    }

    return value
}

export function formatDate(isoString) {
  if (!isoString) return ''

  const date = new Date(isoString)

  const data = date.toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' })

  const hora = date.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false })

  return `${data} ${hora}`
}

export function formatPhone(value) {
    if (!value) return ''
    const digits = value.replace(/\D/g, '')

    if (digits.length === 11) {
        return digits.replace(/^(\d{2})(\d{5})(\d{4})$/, '($1) $2-$3')
    }

    if (digits.length === 10) {
        return digits.replace(/^(\d{2})(\d{4})(\d{4})$/, '($1) $2-$3')
    }

    return value
}

export function formatarCEP(cep) {
  if (!cep) return ''

  cep = cep.replace(/\D/g, '')

  if (cep.length > 5) {
    return cep.substring(0, 5) + '-' + cep.substring(5, 8)
  }

  return cep
}

export function onlyDigits(str) {
  return str.replace(/[^0-9]/g, '')
}