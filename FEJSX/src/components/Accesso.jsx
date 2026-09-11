import { useState } from 'react'
import { useAuth } from '../hooks/useAuth'

export default function Accesso() {
  const { entra, registrati } = useAuth()
  const [modalitaRegistrazione, setModalitaRegistrazione] = useState(false)
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [errore, setErrore] = useState('')
  const [inCorso, setInCorso] = useState(false)

  const invia = async (e) => {
    e.preventDefault()
    setErrore('')
    setInCorso(true)
    try {
      await (modalitaRegistrazione ? registrati(username, password) : entra(username, password))
    } catch (err) {
      setErrore(err.message)
    } finally {
      setInCorso(false)
    }
  }

  return (
    <div className="accesso">
      <div className="riquadro">
        <div className="marchio">
          <h1 className="titolo-app">Notifiche</h1>
          <p>canali, iscrizioni e avvisi in tempo reale</p>
        </div>

        <div className="scheda">
          <div className="interruttore" style={{ display: 'flex', width: '100%' }}>
            <button
              type="button"
              className={!modalitaRegistrazione ? 'attivo' : ''}
              style={{ flex: 1 }}
              onClick={() => { setModalitaRegistrazione(false); setErrore('') }}
            >
              Accedi
            </button>
            <button
              type="button"
              className={modalitaRegistrazione ? 'attivo' : ''}
              style={{ flex: 1 }}
              onClick={() => { setModalitaRegistrazione(true); setErrore('') }}
            >
              Registrati
            </button>
          </div>

          {errore && <div className="avviso errore">{errore}</div>}

          <form onSubmit={invia}>
            <label className="campo">
              <span>Username</span>
              <input
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                autoComplete="username"
                required
              />
            </label>
            <label className="campo">
              <span>Password</span>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete={modalitaRegistrazione ? 'new-password' : 'current-password'}
                required
              />
            </label>

            {modalitaRegistrazione && (
              <p style={{ margin: '-.4rem 0 1rem', fontSize: '.78rem', color: 'var(--testo-tenue)' }}>
                Almeno 8 caratteri. Viene salvata solo come hash BCrypt.
              </p>
            )}

            <button className="btn caldo" style={{ width: '100%' }} disabled={inCorso}>
              {inCorso ? 'Attendi…' : modalitaRegistrazione ? 'Crea account' : 'Entra'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}
