async function request(path, options) {
  const response = await fetch(path, {
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    ...options,
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || `Request failed (${response.status})`)
  }
  return response.json()
}

export const medrouteApi = {
  health: () => request('/api/health'),
  hospitals: () => request('/api/hospitals'),
  createEmergency: (payload) => request('/api/emergencies', {
    method: 'POST',
    body: JSON.stringify(payload),
  }),
}