import { createContext, useContext, useEffect, useState } from 'react'
import api from '../services/api.js'

const AuthContext = createContext(null)

/** Holds the logged-in user (token, role, name) across the whole app. */
export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem('qc_user')) } catch { return null }
  })
  const [loading, setLoading] = useState(!!localStorage.getItem('qc_token') && !localStorage.getItem('qc_user'))

  // If we have a token but no cached user (page refresh), validate it once.
  useEffect(() => {
    const token = localStorage.getItem('qc_token')
    if (token && !localStorage.getItem('qc_user')) {
      api.get('/auth/me')
        .then((res) => {
          const u = { token, ...res.data }
          localStorage.setItem('qc_user', JSON.stringify(u))
          setUser(u)
        })
        .catch(() => {
          localStorage.removeItem('qc_token')
          localStorage.removeItem('qc_user')
        })
        .finally(() => setLoading(false))
    } else {
      setLoading(false)
    }
  }, [])

  const save = (data) => {
    const u = { token: data.token, role: data.role, name: data.name, userId: data.userId }
    localStorage.setItem('qc_token', data.token)
    localStorage.setItem('qc_user', JSON.stringify(u))
    setUser(u)
  }

  const login = async (email, password) => {
    const { data } = await api.post('/auth/login', { email, password })
    save(data)
    return data
  }

  const register = async (payload) => {
    const { data } = await api.post('/auth/register', payload)
    save(data)
    return data
  }

  const logout = () => {
    localStorage.removeItem('qc_token')
    localStorage.removeItem('qc_user')
    setUser(null)
  }

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
