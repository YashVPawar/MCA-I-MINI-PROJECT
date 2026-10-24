import { useEffect, useRef, useState } from 'react'
import L from 'leaflet'
import 'leaflet/dist/leaflet.css'
import { medrouteApi } from './api'
import './layout.css'
import './App.css'

const hospitals = [
  { id: 'sassoon', name: 'Sassoon General Hospital', short: 'Sassoon General', lat: 18.5196, lng: 73.8712, eta: 6, icu: 0, beds: 18, ventilators: 3, blood: true, traffic: 1.1, status: 'ACTIVE' },
  { id: 'ruby', name: 'Ruby Hall Clinic', short: 'Ruby Hall Clinic', lat: 18.5332, lng: 73.8767, eta: 9, icu: 7, beds: 22, ventilators: 5, blood: true, traffic: 1.5, status: 'ACTIVE' },
  { id: 'jehangir', name: 'Jehangir Hospital', short: 'Jehangir Hospital', lat: 18.5293, lng: 73.8761, eta: 8, icu: 0, beds: 12, ventilators: 2, blood: true, traffic: 2.5, status: 'ACTIVE' },
  { id: 'deenanath', name: 'Deenanath Mangeshkar Hospital', short: 'Deenanath Mangeshkar', lat: 18.4635, lng: 73.8075, eta: 18, icu: 8, beds: 28, ventilators: 6, blood: true, traffic: 1.2, status: 'ACTIVE' },
]

const initialCases = [
  { id: 'MR-2048', patient: 'Critical response', severity: 'CRITICAL', status: 'EN ROUTE', hospital: 'Ruby Hall Clinic', eta: 4, blood: 'O+', spo2: 88, lat: 18.525, lng: 73.856, route: [[18.525, 73.856], [18.528, 73.863], [18.531, 73.87], [18.5332, 73.8767]] },
  { id: 'MR-2047', patient: 'Road incident', severity: 'URGENT', status: 'PICKUP', hospital: 'Sassoon General Hospital', eta: 7, blood: 'B+', spo2: 96, lat: 18.513, lng: 73.849 },
  { id: 'MR-2046', patient: 'Medical transfer', severity: 'STABLE', status: 'ARRIVED', hospital: 'Jehangir Hospital', eta: 0, blood: 'A+', spo2: 98, lat: 18.529, lng: 73.873 },
]

const roleLabels = { ADMIN: 'Operations', AMBULANCE: 'Ambulance', HOSPITAL: 'Hospital ER' }

function MapPanel({ cases, hospitals, selectedHospital }) {
  const mapElement = useRef(null)
  const [mapState, setMapState] = useState(null)

  useEffect(() => {
    if (!mapElement.current) return
    const map = L.map(mapElement.current, { zoomControl: false }).setView([18.5204, 73.8567], 13)
    L.control.zoom({ position: 'bottomright' }).addTo(map)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19,
    }).addTo(map)
    const layer = L.layerGroup().addTo(map)
    setMapState({ map, layer })
    return () => map.remove()
  }, [])

  useEffect(() => {
    if (!mapState) return
    const { layer } = mapState
    layer.clearLayers()
    hospitals.forEach((hospital) => {
      L.circleMarker([hospital.lat, hospital.lng], {
        radius: 7,
        color: '#fff',
        weight: 2,
        fillColor: hospital.id === selectedHospital ? '#e5523f' : '#1c766c',
        fillOpacity: 1,
      }).bindTooltip(hospital.short).addTo(layer)
    })
    cases.forEach((emergency) => {
      if (emergency.status === 'ARRIVED') return
      L.circleMarker([emergency.lat, emergency.lng], {
        radius: 6,
        color: '#fff',
        weight: 2,
        fillColor: emergency.severity === 'CRITICAL' ? '#df503c' : '#d29233',
        fillOpacity: 1,
      }).bindTooltip(`${emergency.id} ambulance`).addTo(layer)
      if (emergency.route) {
        L.polyline(emergency.route, { color: '#df503c', weight: 4, opacity: 0.82 }).addTo(layer)
      }
    })
  }, [mapState, cases, hospitals, selectedHospital])

  return <div className="map-wrap"><div className="map-canvas" ref={mapElement} aria-label="Map of Pune hospitals and active ambulances" /><button className="map-center-button" onClick={() => mapState?.map.setView([18.5204, 73.8567], 13)} aria-label="Center map on Pune" title="Center map on Pune">◎</button></div>
}

function recommendHospital(patient, hospitals) {
  const eligible = hospitals.filter((hospital) => hospital.status === 'ACTIVE' && hospital.beds > 0 && (patient.severity !== 'CRITICAL' || hospital.icu > 0) && (patient.spo2 >= 90 || hospital.ventilators > 0) && hospital.blood)
  return eligible.sort((left, right) => (left.eta * 0.5 + left.traffic * 2 - Math.min(left.icu, 5) * 0.2) - (right.eta * 0.5 + right.traffic * 2 - Math.min(right.icu, 5) * 0.2))[0] ?? hospitals[0]
}

function App() {
  const [role, setRole] = useState('AMBULANCE')
  const [activeView, setActiveView] = useState('Dispatch')
  const [cases, setCases] = useState(initialCases)
  const [hospitalData, setHospitalData] = useState(hospitals)
  const [backendStatus, setBackendStatus] = useState('CONNECTING')
  const [apiError, setApiError] = useState('')
  const [showIntake, setShowIntake] = useState(false)
  const [selectedHospital, setSelectedHospital] = useState('ruby')
  const [form, setForm] = useState({ severity: 'CRITICAL', blood: 'O+', spo2: '88', notes: '' })
  const activeCases = cases.filter((item) => item.status !== 'ARRIVED')
  const criticalCount = activeCases.filter((item) => item.severity === 'CRITICAL').length
  const visibleCases = role === 'HOSPITAL'
    ? activeCases.filter((item) => item.hospital === 'Ruby Hall Clinic')
    : activeCases
  const pageTitle = activeView === 'Dispatch'
    ? role === 'HOSPITAL' ? 'Incoming patients' : role === 'ADMIN' ? 'Operations overview' : 'Live dispatch'
    : activeView

  useEffect(() => {
    medrouteApi.health()
      .then(() => setBackendStatus('CONNECTED'))
      .catch(() => setBackendStatus('SIMULATION'))
    medrouteApi.hospitals()
      .then((records) => setHospitalData(records.map((hospital) => ({
        id: hospital.id,
        name: hospital.name,
        short: hospital.name,
        lat: hospital.latitude,
        lng: hospital.longitude,
        eta: hospital.estimatedEtaMinutes,
        icu: hospital.icuBeds,
        beds: hospital.emergencyBeds,
        ventilators: hospital.ventilators,
        blood: Boolean(hospital.bloodGroups),
        traffic: hospital.trafficMultiplier,
        status: hospital.status,
      }))))
      .catch(() => setApiError('Backend unavailable; showing seeded simulation data.'))
  }, [])

  async function createEmergency(event) {
    event.preventDefault()
    const patient = { severity: form.severity, spo2: Number(form.spo2), blood: form.blood }
    const hospital = recommendHospital(patient, hospitalData)
    const ambulance = { lat: 18.5204, lng: 73.8567 }
    let emergency
    try {
      const result = await medrouteApi.createEmergency({
        details: form.notes.trim() || 'New emergency request',
        patient: { severity: patient.severity, spo2: patient.spo2, bloodGroup: patient.blood },
        latitude: ambulance.lat,
        longitude: ambulance.lng,
      })
      const destination = hospitalData.find((item) => item.id === result.hospitalId) ?? hospital
      emergency = {
        id: result.id,
        patient: result.details,
        severity: result.patient.severity,
        status: result.status,
        hospital: result.hospitalName,
        eta: result.etaMinutes,
        blood: result.patient.bloodGroup,
        spo2: result.patient.spo2,
        lat: result.latitude,
        lng: result.longitude,
        route: [[result.latitude, result.longitude], [18.526, 73.864], [destination.lat, destination.lng]],
      }
      setBackendStatus('CONNECTED')
      setApiError('')
    } catch {
      emergency = {
        id: `MR-${2049 + cases.length - initialCases.length}`,
        patient: form.notes.trim() || 'New emergency request',
        severity: patient.severity,
        status: 'ASSIGNED',
        hospital: hospital.name,
        eta: hospital.eta,
        blood: patient.blood,
        spo2: patient.spo2,
        ...ambulance,
        route: [[ambulance.lat, ambulance.lng], [18.526, 73.864], [hospital.lat, hospital.lng]],
      }
      setBackendStatus('SIMULATION')
      setApiError('Emergency saved in this browser only; backend is unavailable.')
    }
    setCases((current) => [emergency, ...current])
    setSelectedHospital(hospital.id)
    setShowIntake(false)
    setActiveView('Dispatch')
  }

  function advanceCase(id) {
    setCases((current) => current.map((item) => item.id === id ? { ...item, status: item.status === 'ASSIGNED' ? 'EN ROUTE' : item.status === 'EN ROUTE' ? 'ARRIVED' : item.status } : item))
  }

  return (
    <main className="app-shell">
      <aside className="sidebar">
        <a className="brand" href="#dispatch" aria-label="MedRoute home">
          <span className="brand-mark">M</span><span>medroute<span className="brand-period">.</span></span>
        </a>
        <div className="sidebar-label">WORKSPACE</div>
        <nav className="primary-nav" aria-label="Main navigation">
          {['Dispatch', 'Emergencies', 'Hospitals', 'Reports'].map((view, index) => (
            <button className={activeView === view ? 'nav-item selected' : 'nav-item'} key={view} onClick={() => setActiveView(view)}>
              <span className="nav-icon">{['⌖', '▤', '＋', '▥'][index]}</span>{view}
              {view === 'Emergencies' && <span className="nav-count">{activeCases.length}</span>}
            </button>
          ))}
        </nav>
        <div className="sidebar-spacer" />
        <div className="simulation-note"><span className="pulse-dot" />SIMULATION MODE</div>
        <button className="profile-switch" onClick={() => setRole((current) => current === 'AMBULANCE' ? 'HOSPITAL' : current === 'HOSPITAL' ? 'ADMIN' : 'AMBULANCE')}>
          <span className="avatar">{role === 'ADMIN' ? 'OP' : role === 'HOSPITAL' ? 'ER' : 'AM'}</span>
          <span className="profile-copy"><strong>{roleLabels[role]}</strong><small>Switch role</small></span>
          <span className="switch-mark">↕</span>
        </button>
      </aside>

      <section className="workspace" id="dispatch">
        <header className="topbar">
          <div className="breadcrumbs">Pune, Maharashtra <span>/</span> <strong>{activeView}</strong></div>
          <div className="topbar-actions"><span className="live-status"><i className={backendStatus === 'CONNECTED' ? '' : 'offline'} /> {backendStatus === 'CONNECTED' ? 'API connected' : backendStatus === 'CONNECTING' ? 'Connecting…' : 'Simulation mode'}</span><button className="icon-button" aria-label="Notifications" title="Notifications">♧<b /></button><span className="top-date">PUNE ZONE</span></div>
        </header>

        <div className="page-content">
          <div className="page-heading">
            <div><p className="eyebrow">{roleLabels[role]} CONSOLE <span>•</span> PUNE ZONE</p><h1>{pageTitle}</h1><p className="heading-subtitle">{role === 'HOSPITAL' ? 'Incoming cases, patient vitals, and receiving capacity.' : role === 'ADMIN' ? 'City-wide response activity and hospital utilization.' : 'Emergency coordination and hospital capacity, in one view.'}</p></div>
            <button className="primary-button" onClick={() => setShowIntake(true)}><span>＋</span> New emergency</button>
          </div>

          <div className="metrics-row">
            <article className="metric"><span className="metric-label">ACTIVE EMERGENCIES</span><strong>{String(activeCases.length).padStart(2, '0')}</strong><span className="metric-foot"><i className="metric-dot red" />{criticalCount} critical response{criticalCount === 1 ? '' : 's'}</span></article>
            <article className="metric"><span className="metric-label">AVAILABLE AMBULANCES</span><strong>08<span className="metric-unit">/12</span></strong><span className="metric-foot"><i className="metric-dot green" />3 units in transit</span></article>
            <article className="metric"><span className="metric-label">HOSPITAL CAPACITY</span><strong>76<span className="metric-unit">%</span></strong><span className="metric-foot"><i className="metric-dot green" />6 facilities accepting</span></article>
            <article className="metric"><span className="metric-label">AVG. RESPONSE ETA</span><strong>08<span className="metric-unit"> min</span></strong><span className="metric-foot"><i className="metric-dot amber" />−2 min vs. daily average</span></article>
          </div>

          {apiError && <p className="api-notice" role="status">{apiError}</p>}

          {activeView === 'Reports' ? <section className="reports-panel"><div className="panel-heading"><div><h2>Emergency history</h2><span>Current simulation session</span></div><span className="report-period">TODAY</span></div><div className="hospital-table-wrap"><table><thead><tr><th>CASE</th><th>SEVERITY</th><th>DESTINATION</th><th>STATUS</th><th>ETA</th><th>SpO₂</th></tr></thead><tbody>{cases.map((item) => <tr key={item.id}><td><strong>{item.id}</strong></td><td><span className={`severity-tag ${item.severity.toLowerCase()}`}>{item.severity}</span></td><td>{item.hospital}</td><td>{item.status}</td><td>{item.eta} min</td><td>{item.spo2}%</td></tr>)}</tbody></table></div></section> : <div className="dispatch-grid">
            <section className="map-panel">
              <div className="panel-heading map-heading"><div><h2>Pune response map</h2><span>Live units and receiving facilities</span></div></div>
              <MapPanel cases={cases} hospitals={hospitalData} selectedHospital={selectedHospital} />
              <div className="map-legend"><span><i className="legend-dot ambulance" />Ambulance</span><span><i className="legend-dot hospital" />Hospital</span><span><i className="legend-line" />Recommended route</span><span className="map-attribution">© OpenStreetMap</span></div>
            </section>

            <section className="case-panel">
              <div className="panel-heading"><div><h2>{role === 'HOSPITAL' ? 'Incoming patients' : 'Active responses'}</h2><span>{role === 'HOSPITAL' ? 'Patients routed to Ruby Hall Clinic' : 'Latest city-wide activity'}</span></div><button className="text-button" onClick={() => setActiveView('Emergencies')}>View all <span>→</span></button></div>
              <div className="case-list">
                {visibleCases.slice(0, 4).map((item) => (
                  <article className="case-item" key={item.id}>
                    <div className="case-topline"><span className={`severity-tag ${item.severity.toLowerCase()}`}>{item.severity}</span><span className="case-id">{item.id}</span></div>
                    <h3>{item.patient}</h3>
                    <p className="case-destination"><span>↗</span> {item.hospital}</p>
                    <div className="case-meta"><span><small>{role === 'HOSPITAL' ? 'BLOOD GROUP' : 'STATUS'}</small><strong>{role === 'HOSPITAL' ? item.blood : item.status}</strong></span><span><small>{role === 'HOSPITAL' ? 'SpO₂' : 'ETA'}</small><strong>{role === 'HOSPITAL' ? `${item.spo2}%` : item.eta ? `${item.eta} min` : 'Arrived'}</strong></span></div>
                    {role === 'HOSPITAL' ? <button className="case-action" onClick={() => setCases((current) => current.map((record) => record.id === item.id ? { ...record, acknowledged: true } : record))}>{item.acknowledged ? 'Acknowledged' : 'Acknowledge patient'} <span>→</span></button> : <button className="case-action" onClick={() => advanceCase(item.id)}>{item.status === 'ASSIGNED' ? 'Start trip' : 'Update status'} <span>→</span></button>}
                  </article>
                ))}
                {activeCases.length === 0 && <p className="empty-state">No active responses. All units are clear.</p>}
              </div>
            </section>
          </div>}

          <section className="hospital-section">
            <div className="panel-heading"><div><h2>Receiving hospitals</h2><span>Capacity and simulated resource availability</span></div><button className="text-button" onClick={() => setActiveView('Hospitals')}>All facilities <span>→</span></button></div>
            <div className="hospital-table-wrap"><table><thead><tr><th>FACILITY</th><th>STATUS</th><th>ICU BEDS</th><th>ER BEDS</th><th>TRAFFIC</th><th>ETA</th></tr></thead><tbody>{hospitalData.map((hospital) => <tr key={hospital.id} className={hospital.id === selectedHospital ? 'recommended-row' : ''}><td><span className="facility-symbol">＋</span><strong>{hospital.name}</strong>{hospital.id === selectedHospital && <span className="recommended-label">RECOMMENDED</span>}</td><td><span className="facility-status"><i />Accepting</span></td><td>{hospital.icu} available</td><td>{hospital.beds} available</td><td><span className={`traffic-state ${hospital.traffic > 2 ? 'heavy' : hospital.traffic > 1.3 ? 'moderate' : ''}`}><i />{hospital.traffic > 2 ? 'Heavy' : hospital.traffic > 1.3 ? 'Moderate' : 'Normal'}</span></td><td className="eta-cell">{hospital.eta} min</td></tr>)}</tbody></table></div>
            <p className="data-disclaimer">Operational decision support only. Patient vitals, traffic conditions, and resource counts are simulated.</p>
          </section>
        </div>
      </section>

      {showIntake && <div className="modal-backdrop" onMouseDown={(event) => event.target === event.currentTarget && setShowIntake(false)}><section className="intake-modal" role="dialog" aria-modal="true" aria-labelledby="intake-title"><div className="modal-heading"><div><p className="eyebrow">NEW INCIDENT</p><h2 id="intake-title">Emergency intake</h2></div><button className="close-button" aria-label="Close" onClick={() => setShowIntake(false)}>×</button></div><form onSubmit={createEmergency}><label>Incident details<input value={form.notes} onChange={(event) => setForm({ ...form, notes: event.target.value })} placeholder="e.g. Road incident, Deccan Gymkhana" /></label><div className="form-row"><label>Severity<select value={form.severity} onChange={(event) => setForm({ ...form, severity: event.target.value })}><option>CRITICAL</option><option>URGENT</option><option>STABLE</option></select></label><label>Blood group<select value={form.blood} onChange={(event) => setForm({ ...form, blood: event.target.value })}>{['O+', 'O-', 'A+', 'A-', 'B+', 'B-', 'AB+', 'AB-'].map((blood) => <option key={blood}>{blood}</option>)}</select></label></div><label>SpO₂ <span className="label-hint">simulated patient value</span><input type="number" min="50" max="100" value={form.spo2} onChange={(event) => setForm({ ...form, spo2: event.target.value })} required /></label><div className="recommendation-note">Hospital selection weighs travel time, traffic, and available resources. Critical cases require ICU capacity.</div><div className="modal-actions"><button type="button" className="subtle-button" onClick={() => setShowIntake(false)}>Cancel</button><button type="submit" className="primary-button">Assign response <span>→</span></button></div></form></section></div>}
    </main>
  )
}

export default App
