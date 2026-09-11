import { useCallback, useEffect, useState } from 'react'
import { api } from './api/client'
import { AuthProvider } from './context/AuthContext'
import { useAuth } from './hooks/useAuth'
import { useNotificheSocket } from './hooks/useNotificheSocket'
import Accesso from './components/Accesso'
import Canali from './components/Canali'
import Notifiche from './components/Notifiche'
import CreaCanale from './components/CreaCanale'
import CreaNotifica from './components/CreaNotifica'
import PilaToast from './components/Toast'

function Scrivania() {
  const { utente, esci } = useAuth()
  const [notifiche, setNotifiche] = useState([])
  const [nonLette, setNonLette] = useState(0)
  const [statoSocket, setStatoSocket] = useState('disconnesso')
  const [toast, setToast] = useState([])
  const [avviso, setAvviso] = useState(null)
  const [idAppenaArrivate, setIdAppenaArrivate] = useState(new Set())
  const [chiaveCanali, setChiaveCanali] = useState(0)

  const mostraToast = useCallback((titolo, testo) => {
    const id = crypto.randomUUID()
    setToast((prec) => [...prec, { id, titolo, testo }])
    setTimeout(() => setToast((prec) => prec.filter((t) => t.id !== id)), 5000)
  }, [])

  const mostraAvviso = useCallback((messaggio) => {
    setAvviso(messaggio)
    setTimeout(() => setAvviso(null), 4000)
  }, [])

  const ricarica = useCallback(async (annullato = () => false) => {
    try {
      const [pagina, conteggio] = await Promise.all([api.notifiche(), api.conteggioNonLette()])
      if (annullato()) return
      setNotifiche(pagina.contenuto)
      setNonLette(conteggio.nonLette)
    } catch (e) {
      if (!annullato()) mostraAvviso({ tipo: 'errore', testo: e.message })
    }
  }, [mostraAvviso])

  useEffect(() => {
    let annullato = false

    // stessa ragione di Canali: la setState deve stare oltre un await,
    // non nel corpo sincrono dell'effetto.
    const caricaAllAvvio = async () => { await ricarica(() => annullato) }
    caricaAllAvvio()

    return () => { annullato = true }
  }, [ricarica])

  // arrivo in tempo reale: la mettiamo in cima senza rifare la GET
  const suNotifica = useCallback((n) => {
    setNotifiche((prec) => (prec.some((x) => x.id === n.id) ? prec : [n, ...prec]))
    setNonLette((c) => c + 1)
    setIdAppenaArrivate((prec) => new Set(prec).add(n.id))
    mostraToast(n.tipo === 'CANALE' ? 'Nuova sul canale' : 'Nuova notifica', n.message)
  }, [mostraToast])

  useNotificheSocket(true, suNotifica, setStatoSocket)

  const iniziale = utente.username.charAt(0).toUpperCase()

  return (
    <div className="app">
      <header className="intestazione">
        <div>
          <h1 className="titolo-app">Notifiche</h1>
        </div>
        <span className="stato-connessione">
          <span className={`pallino ${statoSocket === 'connesso' ? 'connesso' : ''}`} />
          {statoSocket === 'connesso' ? 'in diretta' : 'riconnessione…'}
        </span>

        <div className="spazio" />

        <span className={`contatore ${nonLette === 0 ? 'zero' : ''}`} title="notifiche non lette">
          {nonLette}
        </span>
        <div style={{ display: 'flex', alignItems: 'center', gap: '.55rem' }}>
          <span
            style={{
              display: 'grid', placeItems: 'center', width: 34, height: 34,
              borderRadius: '50%', fontWeight: 800, color: '#12102b',
              background: 'var(--grad-acido)',
            }}
          >
            {iniziale}
          </span>
          <div style={{ lineHeight: 1.2 }}>
            <div style={{ fontWeight: 700, fontSize: '.9rem' }}>{utente.username}</div>
            <span className={`etichetta ${utente.ruolo.toLowerCase()}`}>{utente.ruolo}</span>
          </div>
        </div>
        <button className="btn fantasma piccolo" onClick={esci}>Esci</button>
      </header>

      {avviso && <div className={`avviso ${avviso.tipo}`}>{avviso.testo}</div>}

      <div className="griglia">
        <div>
          <Notifiche
            notifiche={notifiche}
            nonLette={nonLette}
            idAppenaArrivate={idAppenaArrivate}
            onAggiorna={ricarica}
            onMessaggio={mostraAvviso}
          />
        </div>

        <div>
          <Canali key={chiaveCanali} onMessaggio={mostraAvviso} />
          <CreaCanale onCreato={() => setChiaveCanali((k) => k + 1)} onMessaggio={mostraAvviso} />
          {utente.ruolo === 'ADMIN' && <CreaNotifica onMessaggio={mostraAvviso} />}
        </div>
      </div>

      <PilaToast toast={toast} />
    </div>
  )
}

function Radice() {
  const { utente, caricamento } = useAuth()
  if (caricamento) {
    return <div className="accesso"><p style={{ color: 'var(--testo-tenue)' }}>Caricamento…</p></div>
  }
  return utente ? <Scrivania /> : <Accesso />
}

export default function App() {
  return (
    <AuthProvider>
      <Radice />
    </AuthProvider>
  )
}
