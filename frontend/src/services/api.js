import axios from 'axios'

// All backend calls go through here. The base URL comes from
// frontend/.env (VITE_API_URL) and defaults to the local backend.
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
})

// Attach the JWT to every request.
api.interceptors.request.use((cfg) => {
  const token = localStorage.getItem('qc_token')
  if (token) cfg.headers.Authorization = `Bearer ${token}`
  return cfg
})

// An expired/invalid token boots the user back to the login page.
api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401 && !window.location.pathname.startsWith('/login')) {
      localStorage.removeItem('qc_token')
      localStorage.removeItem('qc_user')
      window.location.href = '/login'
    }
    return Promise.reject(err)
  }
)

export default api

/** Pull a friendly message out of an axios error. */
export function apiError(err, fallback = 'Something went wrong') {
  return err?.response?.data?.message || err?.message || fallback
}
