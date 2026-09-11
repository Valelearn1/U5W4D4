import { useCallback, useEffect, useState } from 'react'
import { api } from '../api/client'

function ElencoCanali({ scheda, onMessaggio, onConteggio }) {
  const [canali, setCanali] = useState([])
  const [idSeguiti, setIdSeguiti] = useState(new Set())
  const [inCorso, setInCorso] = useState(true)

  const carica = useCallback(async () => {
    // gli iscritti servono sempre: sono ciò che decide "Segui" o "Smetti"
    const [elenco, seguiti] = await Promise.all([
      scheda === 'tutti' ? api.canali() : api.canaliIscritto(),
      api.canaliIscritto(),
    ])
    setCanali(elenco.contenuto)
    setIdSeguiti(new Set(seguiti.contenuto.map((c) => c.id)))
    onConteggio(seguiti.totaleElementi)
    setInCorso(false)
  }, [scheda, onConteggio])

  useEffect(() => {
    let annullato = false

    // la funzione asincrona vive dentro l'effetto: ogni setState avviene
    // dopo un await, mai in modo sincrono nel corpo dell'effetto.
    const caricaAllAvvio = async () => {
      try {
        await carica()
      } catch (e) {
        if (!annullato) {
          onMessaggio?.({ tipo: 'errore', testo: e.message })
          setInCorso(false)
        }
      }
    }
    caricaAllAvvio()

    return () => { annullato = true }
  }, [carica, onMessaggio])

  const cambiaIscrizione = async (canale) => {
    const seguito = idSeguiti.has(canale.id)
    try {
      await (seguito ? api.unfollow(canale.id) : api.follow(canale.id))
      onMessaggio?.({
        tipo: 'riuscito',
        testo: seguito ? `Non segui più “${canale.nome}”` : `Ora segui “${canale.nome}”`,
      })
      await carica()
    } catch (e) {
      onMessaggio?.({ tipo: 'errore', testo: e.message })
    }
  }

  if (inCorso) return <div className="vuoto">Caricamento…</div>

  if (canali.length === 0) {
    return (
      <div className="vuoto">
        {scheda === 'tutti' ? 'Nessun canale. Creane uno qui sotto.' : 'Non segui ancora nessun canale.'}
      </div>
    )
  }

  return (
    <div className="elenco">
      {canali.map((c) => {
        const seguito = idSeguiti.has(c.id)
        return (
          <article key={c.id} className={`riga ${seguito ? 'seguito' : ''}`}>
            <div className="corpo">
              <div className="nome">{c.nome}</div>
              <div className="sotto">{c.descrizione || 'nessuna descrizione'}</div>
            </div>
            <button
              className={`btn piccolo ${seguito ? 'fantasma' : ''}`}
              onClick={() => cambiaIscrizione(c)}
            >
              {seguito ? 'Smetti' : 'Segui'}
            </button>
          </article>
        )
      })}
    </div>
  )
}

export default function Canali({ onMessaggio }) {
  const [scheda, setScheda] = useState('tutti')      // 'tutti' | 'iscritto'
  const [quantiSeguiti, setQuantiSeguiti] = useState(0)

  return (
    <section className="scheda">
      <h2 className="sezione-titolo">Canali</h2>

      <div className="interruttore">
        <button className={scheda === 'tutti' ? 'attivo' : ''} onClick={() => setScheda('tutti')}>
          Tutti
        </button>
        <button className={scheda === 'iscritto' ? 'attivo' : ''} onClick={() => setScheda('iscritto')}>
          Che seguo ({quantiSeguiti})
        </button>
      </div>

      {/* la key rimonta l'elenco al cambio scheda: stato di caricamento pulito */}
      <ElencoCanali
        key={scheda}
        scheda={scheda}
        onMessaggio={onMessaggio}
        onConteggio={setQuantiSeguiti}
      />
    </section>
  )
}
