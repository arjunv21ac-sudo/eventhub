import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { getErrorMessage } from '../api/client'
import { useAuth } from '../context/AuthContext'

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [form, setForm] = useState({ name: '', email: '', password: '', role: 'USER' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError('')
    try {
      const user = await register(form)
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
        <h1>Create your account</h1>

        {error && <div className="alert alert-error">{error}</div>}

        <div className="role-picker" role="radiogroup" aria-label="Account type">
          {[
            { value: 'USER', title: '🎫 I want to attend', hint: 'Book tickets for events' },
            { value: 'ORGANIZER', title: '🎤 I organize events', hint: 'Create events and scan tickets' },
          ].map((option) => (
            <label key={option.value} className={`role-option ${form.role === option.value ? 'selected' : ''}`}>
              <input type="radio" name="role" value={option.value} checked={form.role === option.value} onChange={handleChange} />
              <strong>{option.title}</strong>
              <span className="muted small">{option.hint}</span>
            </label>
          ))}
        </div>

        <label className="field">
          <span>Full name</span>
          <input name="name" value={form.name} onChange={handleChange} required maxLength={100} autoComplete="name" />
        </label>
        <label className="field">
          <span>Email</span>
          <input type="email" name="email" value={form.email} onChange={handleChange} required autoComplete="email" />
        </label>
        <label className="field">
          <span>Password</span>
          <input type="password" name="password" value={form.password} onChange={handleChange} required minLength={6} autoComplete="new-password" />
          <small className="muted">At least 6 characters</small>
        </label>

        <button className="btn btn-primary btn-block" disabled={submitting}>
          {submitting ? 'Creating account…' : 'Sign up'}
        </button>
        <p className="center muted">Already have an account? <Link to="/login" state={location.state}>Login</Link></p>
      </form>
    </div>
  )
}
