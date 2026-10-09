import axios from 'axios'

export const AUTH_STORAGE_KEY = 'eventhub_auth'

// In dev, Vite proxies /api to Spring Boot. In production, set VITE_API_URL.
export const API_BASE = import.meta.env.VITE_API_URL || '/api'

const api = axios.create({ baseURL: API_BASE })

// Attach the JWT to every request
api.interceptors.request.use((config) => {
  const saved = localStorage.getItem(AUTH_STORAGE_KEY)
  if (saved) {
    const { token } = JSON.parse(saved)
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// Token expired or invalid: log out and send the user to the login page
api.interceptors.response.use(
  (response) => response,
  (error) => {
    const isAuthCall = error.config?.url?.startsWith('/auth')
    if (error.response?.status === 401 && !isAuthCall) {
      localStorage.removeItem(AUTH_STORAGE_KEY)
      window.location.href = '/login?expired=1'
    }
    return Promise.reject(error)
  },
)

const data = (response) => response.data

export const authApi = {
  login: (credentials) => api.post('/auth/login', credentials).then(data),
  register: (form) => api.post('/auth/register', form).then(data),
}

export const eventApi = {
  search: (q, page, size = 9) => api.get('/events', { params: { q, page, size } }).then(data),
  get: (id) => api.get(`/events/${id}`).then(data),
}

export const bookingApi = {
  book: (eventId, quantity) => api.post('/bookings', { eventId, quantity }).then(data),
  mine: () => api.get('/bookings/my').then(data),
  cancel: (id) => api.post(`/bookings/${id}/cancel`).then(data),
}

export const organizerApi = {
  events: () => api.get('/organizer/events').then(data),
  create: (event) => api.post('/organizer/events', event).then(data),
  update: (id, event) => api.put(`/organizer/events/${id}`, event).then(data),
  remove: (id) => api.delete(`/organizer/events/${id}`),
  stats: (id) => api.get(`/organizer/events/${id}/stats`).then(data),
  checkIn: (ticketCode) => api.post(`/organizer/checkin/${encodeURIComponent(ticketCode)}`).then(data),
}

export const qrImageUrl = (ticketCode) => `${API_BASE}/tickets/${ticketCode}/qr`

// Turns any API error into one readable sentence for the UI
export function getErrorMessage(error) {
  const body = error.response?.data
  if (body?.fieldErrors) return Object.values(body.fieldErrors).join('. ')
  if (body?.message) return body.message
  return 'Could not reach the server. Is the backend running?'
}
