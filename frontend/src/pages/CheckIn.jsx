import { useEffect, useRef, useState } from 'react'
import { Html5Qrcode } from 'html5-qrcode'
import { getErrorMessage, organizerApi } from '../api/client'

const SCANNER_ELEMENT_ID = 'qr-reader'

// Gate check-in screen: scan a ticket QR with the camera, or type the code manually
export default function CheckIn() {
  const scannerRef = useRef(null)
  const busyRef = useRef(false) // ignore extra scans while one is being verified

  const [cameraOn, setCameraOn] = useState(false)
  const [cameraError, setCameraError] = useState('')
  const [manualCode, setManualCode] = useState('')
  const [result, setResult] = useState(null) // { ok: boolean, title, detail }
  const [checkedInCount, setCheckedInCount] = useState(0)

  const verifyTicket = async (code) => {
    if (busyRef.current) return
    busyRef.current = true
    try {
      const response = await organizerApi.checkIn(code.trim())
      setResult({ ok: true, title: response.message, detail: response.eventTitle })
      setCheckedInCount((n) => n + 1)
      navigator.vibrate?.(100)
    } catch (err) {
      setResult({ ok: false, title: 'Entry denied', detail: getErrorMessage(err) })
      navigator.vibrate?.([80, 60, 80])
    }
  }

  const onScanSuccess = (decodedText) => {
    if (busyRef.current) return
    scannerRef.current?.pause(true)
    verifyTicket(decodedText)
  }

  const startCamera = async () => {
    setCameraError('')
    setResult(null)
    busyRef.current = false
    try {
      const scanner = new Html5Qrcode(SCANNER_ELEMENT_ID)
      scannerRef.current = scanner
      await scanner.start({ facingMode: 'environment' }, { fps: 10, qrbox: 240 }, onScanSuccess, () => {})
      setCameraOn(true)
    } catch {
      scannerRef.current = null
      setCameraError('Could not open the camera. Allow camera access, or enter the ticket code below.')
    }
  }

  const stopCamera = async () => {
    const scanner = scannerRef.current
    scannerRef.current = null
    setCameraOn(false)
    if (scanner) {
      try {
        await scanner.stop()
        scanner.clear()
      } catch {
        // already stopped
      }
    }
  }

  const scanNext = () => {
    setResult(null)
    busyRef.current = false
    scannerRef.current?.resume()
  }

  const handleManualSubmit = async (e) => {
    e.preventDefault()
    if (!manualCode.trim()) return
    busyRef.current = false
    await verifyTicket(manualCode)
    busyRef.current = false
    setManualCode('')
  }

  // Release the camera when leaving the page
  useEffect(() => () => { stopCamera() }, [])

  return (
    <div className="checkin-layout">
      <div className="page-header">
        <h1>Scan tickets</h1>
        <span className="badge badge-brand">{checkedInCount} checked in this session</span>
      </div>

      <div className="card scanner-card">
        <div id={SCANNER_ELEMENT_ID} className={`qr-reader ${cameraOn ? '' : 'hidden'}`} />

        {!cameraOn && (
          <div className="scanner-placeholder">
            <p>📷 Point the camera at an attendee's ticket QR code.</p>
            <button className="btn btn-primary" onClick={startCamera}>Start camera</button>
          </div>
        )}
        {cameraOn && <button className="btn btn-ghost btn-sm" onClick={stopCamera}>Stop camera</button>}
        {cameraError && <div className="alert alert-error">{cameraError}</div>}
      </div>

      {result && (
        <div className={`card scan-result ${result.ok ? 'ok' : 'fail'}`} role="status">
          <div className="scan-icon">{result.ok ? '✅' : '⛔'}</div>
          <h2>{result.title}</h2>
          <p>{result.detail}</p>
          {cameraOn && <button className="btn btn-primary" onClick={scanNext}>Scan next ticket</button>}
        </div>
      )}

      <form className="card manual-form" onSubmit={handleManualSubmit}>
        <label className="field">
          <span>No camera? Enter the full ticket code</span>
          <input value={manualCode} onChange={(e) => setManualCode(e.target.value)} placeholder="e.g. 3f2a9c1e-…" />
        </label>
        <button className="btn btn-ghost">Verify</button>
      </form>
    </div>
  )
}
