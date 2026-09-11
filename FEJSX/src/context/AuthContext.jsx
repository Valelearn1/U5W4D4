import { useCallback, useEffect, useState } from 'react'
import { api, leggiToken, rimuoviToken, salvaToken } from '../api/client'
import { AuthContext } from './contestoAuth'

export function AuthProvider({ children }) {
  const [utente, setUtente] = useState(null)
  // lo stato iniziale si calcola durante il render, non con una setState nell'effetto:
  // se non c'è alcun token non c'è nulla da caricare.
  const [caricamento, setCaricamento] = useState(() => Boolean(leggiToken()))

  // se un token c'è, chiediamo al backend chi siamo: serve a sopravvivere
  // al refresh della pagina senza rifare il login.
  useEffect(() => {
    if (!leggiToken()) return

    let annullato = false
    api.io()
      .then((u) => { if (!annullato) setUtente(u) })
      .catch(() => rimuoviToken())            // token scaduto o non più valido
      .finally(() => { if (!annullato) setCaricamento(false) })

    return () => { annullato = true }
  }, [])

  const entra = useCallback(async (username, password) => {
    const risposta = await api.login(username, password)
    salvaToken(risposta.token)
    setUtente({
      id: risposta.utente.id,
      username: risposta.utente.username,
      ruolo: risposta.utente.ruolo,
    })
  }, [])

  const registrati = useCallback(async (username, password) => {
    await api.registrazione(username, password)
    return entra(username, password)          // registrazione + login in un colpo solo
  }, [entra])

  const esci = useCallback(() => {
    rimuoviToken()
    setUtente(null)
  }, [])

  return (
    <AuthContext.Provider value={{ utente, caricamento, entra, registrati, esci }}>
      {children}
    </AuthContext.Provider>
  )
}
