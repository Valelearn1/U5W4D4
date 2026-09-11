import { useEffect, useRef } from 'react'
import { urlWebSocket } from '../api/client'

/**
 * Tiene aperta la connessione WebSocket e richiama onNotifica a ogni messaggio.
 *
 * Nessun topic in query: così questa sessione riceve TUTTE le notifiche
 * dell'utente, comprese quelle di canale. È la "campanella".
 */
export function useNotificheSocket(attivo, onNotifica, onStato) {
  const callbackRef = useRef(null)
  const statoRef = useRef(null)

  // aggiornare un ref durante il render è vietato: lo si fa in un effetto,
  // così le callback restano fresche senza riaprire la connessione a ogni render.
  useEffect(() => {
    callbackRef.current = onNotifica
    statoRef.current = onStato
  }, [onNotifica, onStato])

  useEffect(() => {
    if (!attivo) return

    let ws
    let riconnessione
    let chiuso = false

    const connetti = () => {
      ws = new WebSocket(urlWebSocket())

      ws.onopen = () => statoRef.current?.('connesso')
      ws.onmessage = (e) => callbackRef.current?.(JSON.parse(e.data))
      ws.onclose = () => {
        if (chiuso) return
        statoRef.current?.('disconnesso')
        riconnessione = setTimeout(connetti, 3000)   // il server può essersi riavviato
      }
      ws.onerror = () => ws.close()
    }

    connetti()

    return () => {
      chiuso = true
      clearTimeout(riconnessione)
      ws?.close()
    }
  }, [attivo])
}
