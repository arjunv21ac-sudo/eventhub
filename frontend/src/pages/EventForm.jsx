import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { eventApi, getErrorMessage, organizerApi } from '../api/client'
import { nowForDateTimeInput, toDateTimeInput } from '../utils/format'

const EMPTY_FORM = { title: '', description: '', venue: '', city: '', startTime: '', price: '', totalSeats: '' }

// One form for both "create" (/organizer/events/new) and "edit" (/organizer/events/:id/edit)
export default function EventForm() {
  const { id } = useParams()
  const isEdit = Boolean(id)
  const navigate = useNavigate()

  const [form, setForm] = useState(EMPTY_FORM)
  const [loading, setLoading] = useState(isEdit)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (!isEdit) return
    eventApi.get(id)
      .then((event) => setForm({
        title: event.title,
        description: event.description || '',
        venue: event.venue,
        city: event.city,
        startTime: toDateTimeInput(event.startTime),
        price: event.price,
        totalSeats: event.totalSeats,
      }))
      .catch((err) => setError(getErrorMessage(err)))
      .finally(() => setLoading(false))
  }, [id, isEdit])

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    setError('')
    const payload = { ...form, price: Number(form.price), totalSeats: Number(form.totalSeats) }
    try {
      if (isEdit) await organizerApi.update(id, payload)
      else await organizerApi.create(payload)
      navigate('/organizer')
    } catch (err) {
      setError(getErrorMessage(err))
      setSubmitting(false)
    }
  }

  if (loading) return <p className="center muted">Loading…</p>

  return (
    <form className="card form-card" onSubmit={handleSubmit}>
      <Link to="/organizer" className="muted small">← Dashboard</Link>
      <h1>{isEdit ? 'Edit event' : 'Create a new event'}</h1>

      {error && <div className="alert alert-error">{error}</div>}

      <label className="field">
        <span>Event title</span>
        <input name="title" value={form.title} onChange={handleChange} required maxLength={150} placeholder="e.g. Spring Boot Developer Meetup" />
      </label>
      <label className="field">
        <span>Description</span>
        <textarea name="description" value={form.description} onChange={handleChange} rows={4} maxLength={2000} placeholder="What should attendees expect?" />
      </label>

      <div className="form-row">
        <label className="field">
          <span>Venue</span>
          <input name="venue" value={form.venue} onChange={handleChange} required />
        </label>
        <label className="field">
          <span>City</span>
          <input name="city" value={form.city} onChange={handleChange} required maxLength={100} />
        </label>
      </div>

      <div className="form-row">
        <label className="field">
          <span>Date & time</span>
          <input type="datetime-local" name="startTime" value={form.startTime} onChange={handleChange} required min={nowForDateTimeInput()} />
        </label>
        <label className="field">
          <span>Ticket price (₹)</span>
          <input type="number" name="price" value={form.price} onChange={handleChange} required min="0" step="1" placeholder="0 for free" />
        </label>
        <label className="field">
          <span>Total seats</span>
          <input type="number" name="totalSeats" value={form.totalSeats} onChange={handleChange} required min="1" max="100000" />
        </label>
      </div>

      <div className="row-gap">
        <button className="btn btn-primary" disabled={submitting}>
          {submitting ? 'Saving…' : isEdit ? 'Save changes' : 'Create event'}
        </button>
        <Link to="/organizer" className="btn btn-ghost">Cancel</Link>
      </div>
    </form>
  )
}
