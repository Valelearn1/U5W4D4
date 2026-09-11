import { api } from '../api/client'

const quandoInBreve = (iso) => {
  const secondi = Math.floor((Date.now() - new Date(iso)) / 1000)
  if (secondi < 60) return 'adesso'
  if (secondi < 3600) return `${Math.floor(secondi / 60)} min fa`
  if (secondi < 86400) return `${Math.floor(secondi / 3600)} h fa`
  return new Date(iso).toLocaleDateString('it-IT')
}

export default function Notifiche({ notifiche, nonLette, idAppenaArrivate, onAggiorna, onMessaggio }) {
  const segnaLetta = async (n) => {
    if (n.readAt) return
    try {
      await api.segnaLetta(n.id)
      onAggiorna()
    } catch (e) {
      onMessaggio?.({ tipo: 'errore', testo: e.message })
    }
  }

  const segnaTutte = async () => {
    try {
      const esito = await api.segnaTutteLette()
      onMessaggio?.({ tipo: 'riuscito', testo: `${esito.aggiornate} notifiche segnate come lette` })
      onAggiorna()
    } catch (e) {
      onMessaggio?.({ tipo: 'errore', testo: e.message })
    }
  }

  return (
    <section className="scheda">
      <h2 className="sezione-titolo">
        Notifiche
        <span className={`contatore ${nonLette === 0 ? 'zero' : ''}`}>{nonLette}</span>
      </h2>

      {nonLette > 0 && (
        <button className="btn fantasma piccolo" style={{ marginBottom: '1rem' }} onClick={segnaTutte}>
          Segna tutte come lette
        </button>
      )}

      {notifiche.length === 0 && (
        <div className="vuoto">Nessuna notifica. Segui un canale per iniziare a riceverne.</div>
      )}

      <div className="elenco">
        {notifiche.map((n) => (
          <article
            key={n.id}
            className={[
              'riga',
              !n.readAt ? 'non-letta' : '',
              idAppenaArrivate.has(n.id) ? 'appena-arrivata' : '',
            ].join(' ')}
            onClick={() => segnaLetta(n)}
            style={{ cursor: n.readAt ? 'default' : 'pointer' }}
            title={n.readAt ? 'già letta' : 'clicca per segnare come letta'}
          >
            <div className="corpo">
              <div style={{ display: 'flex', alignItems: 'center', gap: '.5rem', marginBottom: '.25rem' }}>
                <span className={`etichetta ${n.tipo.toLowerCase()}`}>{n.tipo}</span>
                <span className="sotto" style={{ fontSize: '.72rem' }}>{quandoInBreve(n.createdAt)}</span>
              </div>
              <div className="nome" style={{ fontWeight: n.readAt ? 400 : 700 }}>{n.message}</div>
            </div>
            {!n.readAt && <span className="pallino connesso" />}
          </article>
        ))}
      </div>
    </section>
  )
}
