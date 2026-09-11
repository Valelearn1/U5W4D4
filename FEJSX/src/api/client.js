const BASE = 'http://localhost:8080'

const CHIAVE_TOKEN = 'u5w4d4.token'

export const leggiToken = () => localStorage.getItem(CHIAVE_TOKEN)
export const salvaToken = (t) => localStorage.setItem(CHIAVE_TOKEN, t)
export const rimuoviToken = () => localStorage.removeItem(CHIAVE_TOKEN)

/** Errore che porta con sé lo stato HTTP, così i componenti possono distinguere 401 da 403. */
export class ErroreApi extends Error {
  constructor(stato, messaggio) {
    super(messaggio)
    this.stato = stato
  }
}

async function richiesta(metodo, percorso, corpo) {
  const token = leggiToken()
  const risposta = await fetch(`${BASE}${percorso}`, {
    method: metodo,
    headers: {
      ...(corpo ? { 'Content-Type': 'application/json' } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    ...(corpo ? { body: JSON.stringify(corpo) } : {}),
  })

  if (risposta.status === 204) return null

  const testo = await risposta.text()
  const dati = testo ? JSON.parse(testo) : null

  if (!risposta.ok) {
    // il backend risponde in formato ProblemDetail (RFC 9457)
    throw new ErroreApi(risposta.status, dati?.detail || dati?.title || 'Errore imprevisto')
  }
  return dati
}

export const api = {
  registrazione: (username, password) =>
    richiesta('POST', '/api/auth/registrazione', { username, password }),
  login: (username, password) =>
    richiesta('POST', '/api/auth/login', { username, password }),
  io: () => richiesta('GET', '/api/auth/io'),

  canali: (page = 0) => richiesta('GET', `/api/canali?page=${page}`),
  canaliIscritto: (page = 0) => richiesta('GET', `/api/canali/iscritto?page=${page}`),
  creaCanale: (nome, descrizione) => richiesta('POST', '/api/canali', { nome, descrizione }),
  follow: (canaleId) => richiesta('POST', `/api/iscrizioni/${canaleId}`),
  unfollow: (canaleId) => richiesta('DELETE', `/api/iscrizioni/${canaleId}`),

  notifiche: (page = 0) => richiesta('GET', `/api/notifiche?page=${page}`),
  conteggioNonLette: () => richiesta('GET', '/api/notifiche/non-lette/conteggio'),
  segnaLetta: (id) => richiesta('PATCH', `/api/notifiche/${id}/letta`),
  segnaTutteLette: () => richiesta('POST', '/api/notifiche/lette'),
  creaNotifica: (corpo) => richiesta('POST', '/api/notifiche', corpo),
}

/** URL del canale WebSocket: il token va in query perché l'handshake non accetta header. */
export const urlWebSocket = () => `ws://localhost:8080/ws/notifiche?token=${leggiToken()}`
