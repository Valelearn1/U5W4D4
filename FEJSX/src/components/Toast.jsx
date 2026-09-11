export default function PilaToast({ toast }) {
  if (toast.length === 0) return null
  return (
    <div className="pila-toast">
      {toast.map((t) => (
        <div key={t.id} className="toast">
          <strong>{t.titolo}</strong>
          {t.testo}
        </div>
      ))}
    </div>
  )
}
