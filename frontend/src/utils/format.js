const rupees = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 })

export function formatPrice(amount) {
  return Number(amount) === 0 ? 'Free' : rupees.format(amount)
}

export function formatMoney(amount) {
  return rupees.format(amount ?? 0)
}

// "Sat, 18 Oct 2026, 6:00 pm"
export function formatDateTime(iso) {
  return new Date(iso).toLocaleString('en-IN', {
    weekday: 'short',
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })
}

// { day: "18", month: "OCT" } for the date badge on event cards
export function dayAndMonth(iso) {
  const date = new Date(iso)
  return {
    day: date.getDate(),
    month: date.toLocaleString('en-IN', { month: 'short' }).toUpperCase(),
  }
}

// "2026-10-18T18:00:00" -> "2026-10-18T18:00" (the format <input type="datetime-local"> needs)
export function toDateTimeInput(iso) {
  return iso ? iso.slice(0, 16) : ''
}

export function nowForDateTimeInput() {
  const now = new Date()
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset())
  return now.toISOString().slice(0, 16)
}
