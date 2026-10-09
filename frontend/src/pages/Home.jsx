import { useEffect, useState } from 'react'
import { eventApi, getErrorMessage } from '../api/client'
import EventCard from '../components/EventCard'

export default function Home() {
  const [searchInput, setSearchInput] = useState('')
  const [query, setQuery] = useState('')
  const [page, setPage] = useState(0)
  const [result, setResult] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  // Re-fetch whenever the search text or page changes
  useEffect(() => {
    let ignore = false // prevents an older, slower response from overwriting a newer one
    setLoading(true)
    setError('')
    eventApi.search(query, page)
      .then((data) => { if (!ignore) setResult(data) })
      .catch((err) => { if (!ignore) setError(getErrorMessage(err)) })
      .finally(() => { if (!ignore) setLoading(false) })
    return () => { ignore = true }
  }, [query, page])

  const handleSearch = (e) => {
    e.preventDefault()
    setPage(0)
    setQuery(searchInput.trim())
  }

  return (
    <>
      <section className="hero">
        <h1>Find events. Book in seconds. Walk in with a QR.</h1>
        <p>Concerts, meetups, comedy and more, happening near you.</p>
        <form className="search-bar" onSubmit={handleSearch}>
          <input
            type="search"
            placeholder="Search by event name or city…"
            value={searchInput}
            onChange={(e) => setSearchInput(e.target.value)}
            aria-label="Search events"
          />
          <button className="btn btn-primary" type="submit">Search</button>
        </form>
      </section>

      {error && <div className="alert alert-error">{error}</div>}

      {loading && !result && <p className="center muted">Loading events…</p>}

      {result && result.content.length === 0 && (
        <div className="empty">
          <p>No upcoming events{query && <> matching “{query}”</>}.</p>
        </div>
      )}

      {result && result.content.length > 0 && (
        <>
          <div className={`event-grid ${loading ? 'is-loading' : ''}`}>
            {result.content.map((event) => <EventCard key={event.id} event={event} />)}
          </div>

          {result.totalPages > 1 && (
            <div className="pagination">
              <button className="btn btn-ghost" disabled={page === 0} onClick={() => setPage(page - 1)}>← Previous</button>
              <span className="muted">Page {page + 1} of {result.totalPages}</span>
              <button className="btn btn-ghost" disabled={page + 1 >= result.totalPages} onClick={() => setPage(page + 1)}>Next →</button>
            </div>
          )}
        </>
      )}
    </>
  )
}
