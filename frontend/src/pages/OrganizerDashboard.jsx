import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { getErrorMessage, organizerApi } from '../api/client'
import { formatDateTime, formatMoney } from '../utils/format'

export default function OrganizerDashboard() {
  const [rows, setRows] = useState(null) // [{ event, stats }]
  const [error, setError] = useState('')

  const loadDashboard = async () => {
    try {
      const events = await organizerApi.events()
      // Load stats for every event in parallel
      const stats = await Promise.all(events.map((e) => organizerApi.stats(e.id)))
      setRows(events.map((event, i) => ({ event, stats: stats[i] })))
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  useEffect(() => {
    loadDashboard()
  }, [])

  const handleDelete = async (event) => {
    if (!window.confirm(`Delete "${event.title}"? This cannot be undone.`)) return
    setError('')
    try {
      await organizerApi.remove(event.id)
      setRows((list) => list.filter((row) => row.event.id !== event.id))
    } catch (err) {
      setError(getErrorMessage(err))
    }
  }

  if (!rows && !error) return <p className="center muted">Loading dashboard…</p>

  const totals = (rows || []).reduce(
    (sum, { stats }) => ({
      sold: sum.sold + stats.ticketsSold,
      checkedIn: sum.checkedIn + stats.checkedIn,
      revenue: sum.revenue + Number(stats.revenue),
    }),
    { sold: 0, checkedIn: 0, revenue: 0 },
  )

  return (
    <>
      <div className="page-header">
        <h1>Organizer Dashboard</h1>
        <div className="row-gap">
          <Link to="/organizer/checkin" className="btn btn-ghost">📷 Scan tickets</Link>
          <Link to="/organizer/events/new" className="btn btn-primary">+ Create event</Link>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      {rows && (
        <div className="stat-grid">
          <div className="card stat"><span className="muted">Events</span><strong>{rows.length}</strong></div>
          <div className="card stat"><span className="muted">Tickets sold</span><strong>{totals.sold}</strong></div>
          <div className="card stat"><span className="muted">Checked in</span><strong>{totals.checkedIn}</strong></div>
          <div className="card stat"><span className="muted">Revenue</span><strong>{formatMoney(totals.revenue)}</strong></div>
        </div>
      )}

      {rows?.length === 0 && (
        <div className="empty">
          <p>You haven't created any events yet.</p>
          <Link to="/organizer/events/new" className="btn btn-primary">Create your first event</Link>
        </div>
      )}

      {rows?.length > 0 && (
        <div className="card table-wrap">
          <table className="table">
            <thead>
              <tr>
                <th>Event</th>
                <th>Date</th>
                <th>Sold</th>
                <th>Checked in</th>
                <th>Revenue</th>
                <th aria-label="Actions" />
              </tr>
            </thead>
            <tbody>
              {rows.map(({ event, stats }) => {
                const past = new Date(event.startTime) < new Date()
                const percent = Math.round((stats.ticketsSold / event.totalSeats) * 100)
                return (
                  <tr key={event.id}>
                    <td>
                      <Link to={`/events/${event.id}`}><strong>{event.title}</strong></Link>
                      <div className="muted small">{event.city}{past && ' · Ended'}</div>
                    </td>
                    <td className="nowrap">{formatDateTime(event.startTime)}</td>
                    <td>
                      {stats.ticketsSold} / {event.totalSeats}
                      <div className="progress" title={`${percent}% sold`}><div style={{ width: `${percent}%` }} /></div>
                    </td>
                    <td>{stats.checkedIn}</td>
                    <td className="nowrap">{formatMoney(stats.revenue)}</td>
                    <td className="nowrap actions">
                      <Link to={`/organizer/events/${event.id}/edit`} className="btn btn-ghost btn-sm">Edit</Link>
                      <button className="btn btn-danger-outline btn-sm" onClick={() => handleDelete(event)}>Delete</button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
    </>
  )
}
