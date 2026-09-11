import { useState } from 'react'
import { api } from '../api/client'

export default function CreaCanale({ onCreato, onMessaggio }) {
  const [nome, setNome] = useState('')
  const [descrizione, setDescrizione] = useState('')
  const [inCorso, setInCorso] = useState(false)

  const invia = async (e) => {
    e.preventDefault()
    setInCorso(true)
    try {
      const canale = await api.creaCanale(nome, descrizione)
      onMessaggio?.({ tipo: 'riuscito', testo: `Canale “${canale.nome}” creato` })
      setNome('')
      setDescrizione('')
      onCreato?.()
    } catch (err) {
      onMessaggio?.({ tipo: 'errore', testo: err.message })
    } finally {
      setInCorso(false)
    }
  }

  return (
    <section className="scheda">
      <h2 className="sezione-titolo">Nuovo canale</h2>
      <form onSubmit={invia}>
        <label className="campo">
          <span>Nome</span>
          <input value={nome} onChange={(e) => setNome(e.target.value)} required />
        </label>
        <label className="campo">
          <span>Descrizione</span>
          <input value={descrizione} onChange={(e) => setDescrizione(e.target.value)} />
        </label>
        <button className="btn freddo" disabled={inCorso}>
          {inCorso ? 'Creo…' : 'Crea canale'}
        </button>
      </form>
    </section>
  )
}
