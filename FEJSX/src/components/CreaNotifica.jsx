import { useEffect, useState } from 'react'
import { api } from '../api/client'
import { useAuth } from '../hooks/useAuth'

/** Pannello riservato agli ADMIN: è l'unico modo di creare notifiche. */
export default function CreaNotifica({ onMessaggio }) {
  const { utente } = useAuth()
  const [tipo, setTipo] = useState('ALL')
  const [idCanale, setIdCanale] = useState('')
  const [idDestinatario, setIdDestinatario] = useState('')
  const [message, setMessage] = useState('')
  const [canali, setCanali] = useState([])
  const [inCorso, setInCorso] = useState(false)

  useEffect(() => {
    api.canali().then((p) => setCanali(p.contenuto)).catch(() => {})
  }, [])

  const invia = async (e) => {
    e.preventDefault()
    setInCorso(true)
    try {
      const create = await api.creaNotifica({
        tipo,
        message,
        idCanale: tipo === 'CANALE' ? idCanale : null,
        idDestinatario: tipo === 'PERSONAL' ? idDestinatario : null,
      })
      onMessaggio?.({
        tipo: 'riuscito',
        testo: `${create.length} notific${create.length === 1 ? 'a creata' : 'he create'}`,
      })
      setMessage('')
    } catch (err) {
      onMessaggio?.({ tipo: 'errore', testo: err.message })
    } finally {
      setInCorso(false)
    }
  }

  const spiegazione = {
    ALL: 'Una notifica per ogni utente registrato.',
    CANALE: 'Una notifica per ogni iscritto al canale scelto.',
    PERSONAL: 'Una sola notifica, per il destinatario indicato.',
  }[tipo]

  return (
    <section className="scheda">
      <h2 className="sezione-titolo">
        Invia notifica <span className="etichetta admin">admin</span>
      </h2>

      <form onSubmit={invia}>
        <label className="campo">
          <span>Tipo</span>
          <select value={tipo} onChange={(e) => setTipo(e.target.value)}>
            <option value="ALL">ALL — a tutti gli utenti</option>
            <option value="CANALE">CANALE — agli iscritti di un canale</option>
            <option value="PERSONAL">PERSONAL — a un singolo utente</option>
          </select>
        </label>

        <p style={{ margin: '-.5rem 0 .9rem', fontSize: '.78rem', color: 'var(--testo-tenue)' }}>
          {spiegazione}
        </p>

        {tipo === 'CANALE' && (
          <label className="campo">
            <span>Canale</span>
            <select value={idCanale} onChange={(e) => setIdCanale(e.target.value)} required>
              <option value="">— scegli —</option>
              {canali.map((c) => <option key={c.id} value={c.id}>{c.nome}</option>)}
            </select>
          </label>
        )}

        {tipo === 'PERSONAL' && (
          <label className="campo">
            <span>ID destinatario</span>
            <input
              value={idDestinatario}
              onChange={(e) => setIdDestinatario(e.target.value)}
              placeholder="UUID utente"
              required
            />
            <button
              type="button"
              className="btn fantasma piccolo"
              style={{ marginTop: '.45rem' }}
              onClick={() => setIdDestinatario(utente.id)}
            >
              usa il mio ID
            </button>
          </label>
        )}

        <label className="campo">
          <span>Messaggio</span>
          <textarea value={message} onChange={(e) => setMessage(e.target.value)} required />
        </label>

        <button className="btn caldo" disabled={inCorso}>
          {inCorso ? 'Invio…' : 'Invia'}
        </button>
      </form>
    </section>
  )
}
