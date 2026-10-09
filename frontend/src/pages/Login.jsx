import { useState } from 'react'
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../context/AuthContext'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [searchParams] = useSearchParams()

  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const user = await login(form)
      // Go back to the page that asked for login, else to the right home page
      const fallback = user.role === 'ORGANIZER' ? '/organizer' : '/'
      navigate(location.state?.from || fallback, { replace: true })
    } catch (err) {
      setError(getErrorMessage(err))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-page">
      <form className="card auth-card" onSubmit={handleSubmit}>
        <h1>Welcome back</h1>
        <p className="muted">Login to book tickets and see your QR codes.</p>

        {searchParams.get('expired') && <div className="alert alert-info">Your session expired. Please login again.</div>}
        {error && <div className="alert alert-error">{error}</div>}

        <label className="field">
          <span>Email</span>
          <input type="email" name="email" value={form.email} onChange={handleChange} required autoComplete="email" />
        </label>
        <label className="field">
          <span>Password</span>
          <input type="password" name="password" value={form.password} onChange={handleChange} required autoComplete="current-password" />
        </label>

        <button className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? 'Logging in…' : 'Login'}
        </button>
        <p className="center muted">New here? <Link to="/register" state={location.state}>Create an account</Link></p>
      </form>
    </div>
  )
}
