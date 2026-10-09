import { lazy, Suspense } from 'react'
import { Route, Routes } from 'react-router-dom'
import Navbar from './components/Navbar'
import ProtectedRoute from './components/ProtectedRoute'
import Home from './pages/Home'
import EventDetails from './pages/EventDetails'
import Login from './pages/Login'
import Register from './pages/Register'
import MyBookings from './pages/MyBookings'
import OrganizerDashboard from './pages/OrganizerDashboard'
import EventForm from './pages/EventForm'
import NotFound from './pages/NotFound'

// Code splitting: the camera/QR library is large and only organizers need it,
// so it is downloaded only when the Scan page is opened
const CheckIn = lazy(() => import('./pages/CheckIn'))

export default function App() {
  return (
    <>
      <Navbar />
      <main className="container">
        <Suspense fallback={<p className="center muted">Loading…</p>}>
          <Routes>
            {/* Public */}
            <Route path="/" element={<Home />} />
            <Route path="/events/:id" element={<EventDetails />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* Any logged-in user */}
            <Route path="/my-bookings" element={<ProtectedRoute><MyBookings /></ProtectedRoute>} />

            {/* Organizers only */}
            <Route path="/organizer" element={<ProtectedRoute role="ORGANIZER"><OrganizerDashboard /></ProtectedRoute>} />
            <Route path="/organizer/events/new" element={<ProtectedRoute role="ORGANIZER"><EventForm /></ProtectedRoute>} />
            <Route path="/organizer/events/:id/edit" element={<ProtectedRoute role="ORGANIZER"><EventForm /></ProtectedRoute>} />
            <Route path="/organizer/checkin" element={<ProtectedRoute role="ORGANIZER"><CheckIn /></ProtectedRoute>} />

            <Route path="*" element={<NotFound />} />
          </Routes>
        </Suspense>
      </main>
    </>
  )
}
