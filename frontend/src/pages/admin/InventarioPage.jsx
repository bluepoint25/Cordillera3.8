import { useState, useEffect, useMemo } from 'react'
import Navbar from '../../components/Navbar'
import { inventarioApi } from '../../api/apiClient'

const FORM_VACIO = { productoId: '', nombre: '', stock: '' }

// Semáforo de stock: 🔴 bajo (<20) · 🟡 medio (20–49) · 🟢 bien (≥50)
const UMBRAL_ROJO  = 20
const UMBRAL_VERDE = 50

function semaforoStock(stock) {
  const s = stock ?? 0
  if (s < UMBRAL_ROJO)  return { emoji: '🔴', label: 'Bajo',  badge: 'badge-red',   key: 'rojo' }
  if (s < UMBRAL_VERDE) return { emoji: '🟡', label: 'Medio', badge: 'badge-gold',  key: 'amarillo' }
  return                       { emoji: '🟢', label: 'Bien',  badge: 'badge-green', key: 'verde' }
}

export default function InventarioPage() {
  const [items, setItems]       = useState([])
  const [loading, setLoading]   = useState(true)
  const [error, setError]       = useState('')
  const [success, setSuccess]   = useState('')
  const [showForm, setShowForm] = useState(false)
  const [editandoId, setEditandoId] = useState(null)
  const [saving, setSaving]     = useState(false)
  const [form, setForm]         = useState(FORM_VACIO)

  useEffect(() => { cargar() }, [])

  const cargar = async () => {
    try {
      setLoading(true)
      const res = await inventarioApi.listar()
      setItems(Array.isArray(res.data) ? res.data : [])
    } catch { setError('No se pudo conectar con el servicio de inventario') }
    finally { setLoading(false) }
  }

  const stats = useMemo(() => {
    const conteo = { verde: 0, amarillo: 0, rojo: 0 }
    items.forEach(i => { conteo[semaforoStock(i.stock).key]++ })
    return {
      productos: items.length,
      unidades:  items.reduce((acc, i) => acc + (i.stock || 0), 0),
      ...conteo,
    }
  }, [items])

  const abrirCrear = () => {
    setEditandoId(null)
    setForm(FORM_VACIO)
    setShowForm(true)
    setError(''); setSuccess('')
  }

  const abrirEditar = (i) => {
    setEditandoId(i.id)
    setForm({ productoId: i.productoId, nombre: i.nombre, stock: i.stock })
    setShowForm(true)
    setError(''); setSuccess('')
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const cerrarForm = () => {
    setShowForm(false)
    setEditandoId(null)
    setForm(FORM_VACIO)
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true); setError(''); setSuccess('')
    const payload = {
      productoId: Number(form.productoId),
      nombre:     form.nombre,
      stock:      Number(form.stock),
    }
    try {
      if (editandoId) {
        await inventarioApi.actualizar(editandoId, payload)
        setSuccess('✅ Producto actualizado correctamente')
      } else {
        await inventarioApi.crear(payload)
        setSuccess('✅ Producto creado correctamente')
      }
      cerrarForm()
      cargar()
    } catch (err) {
      setError(err.response?.data?.message || err.response?.data?.error || 'Error al guardar el producto')
    } finally { setSaving(false) }
  }

  const handleEliminar = async (i) => {
    if (!window.confirm(`¿Eliminar el producto "${i.nombre}" del inventario?`)) return
    setError(''); setSuccess('')
    try {
      await inventarioApi.eliminar(i.id)
      setSuccess('🗑️ Producto eliminado')
      if (editandoId === i.id) cerrarForm()
      cargar()
    } catch (err) {
      setError(err.response?.data?.message || 'No se pudo eliminar el producto')
    }
  }

  return (
    <div className="page-wrapper">
      <Navbar />
      <div className="page-content">

        {/* Header */}
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:'1.5rem' }}>
          <div>
            <h1 className="page-title">📦 Mantenedor de Inventario</h1>
            <p className="page-subtitle">Administra los productos y el stock disponible</p>
          </div>
          <button className="btn btn-primary" onClick={showForm ? cerrarForm : abrirCrear}>
            {showForm ? '✕ Cancelar' : '+ Nuevo producto'}
          </button>
        </div>

        {error   && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        {/* Stats */}
        <div className="grid-3" style={{ marginBottom:'1.5rem' }}>
          <div className="stat-card">
            <div className="stat-value">{stats.productos}</div>
            <div className="stat-label">Productos</div>
          </div>
          <div className="stat-card">
            <div className="stat-value">{stats.unidades.toLocaleString('es-CL')}</div>
            <div className="stat-label">Unidades en stock</div>
          </div>
          <div className="stat-card">
            <div className="stat-value" style={{ fontSize: '1.4rem' }}>
              🟢 {stats.verde} · 🟡 {stats.amarillo} · 🔴 {stats.rojo}
            </div>
            <div className="stat-label">Semáforo de stock</div>
          </div>
        </div>

        {/* Leyenda del semáforo */}
        <div style={{
          display:'flex', gap:'1.25rem', flexWrap:'wrap', alignItems:'center',
          background:'#F8F9FA', border:'1px solid var(--border)', borderRadius:8,
          padding:'.5rem .9rem', marginBottom:'1.5rem', fontSize:'.85rem',
        }}>
          <strong style={{ color:'var(--text-lt)' }}>Semáforo de stock:</strong>
          <span>🟢 Bien (≥ {UMBRAL_VERDE})</span>
          <span>🟡 Medio ({UMBRAL_ROJO}–{UMBRAL_VERDE - 1})</span>
          <span>🔴 Bajo (&lt; {UMBRAL_ROJO})</span>
        </div>

        {/* Formulario */}
        {showForm && (
          <div className="card" style={{ marginBottom:'1.5rem' }}>
            <div className="card-header">
              <span className="card-title">{editandoId ? 'Editar producto' : 'Nuevo producto'}</span>
            </div>
            <form onSubmit={handleSubmit}>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">ID de producto</label>
                  <input className="form-input" type="number" min={1} required
                    value={form.productoId}
                    onChange={e => setForm(f => ({ ...f, productoId: e.target.value }))} />
                </div>
                <div className="form-group">
                  <label className="form-label">Stock</label>
                  <input className="form-input" type="number" min={0} required
                    value={form.stock}
                    onChange={e => setForm(f => ({ ...f, stock: e.target.value }))} />
                </div>
              </div>

              <div className="form-group">
                <label className="form-label">Nombre del producto</label>
                <input className="form-input" required minLength={2}
                  placeholder="Ej: Notebook Premium 15&quot;"
                  value={form.nombre}
                  onChange={e => setForm(f => ({ ...f, nombre: e.target.value }))} />
              </div>

              <div className="flex-end" style={{ gap:'.75rem' }}>
                <button type="button" className="btn btn-outline" onClick={cerrarForm}>Cancelar</button>
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {saving ? 'Guardando...' : (editandoId ? '💾 Guardar cambios' : '+ Crear producto')}
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Tabla */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">Productos en inventario</span>
            <button className="btn btn-outline" onClick={cargar}
              style={{ padding:'.4rem .8rem', fontSize:'.85rem' }}>
              🔄 Actualizar
            </button>
          </div>
          {loading ? (
            <div className="loading">Cargando inventario...</div>
          ) : items.length === 0 ? (
            <div style={{ textAlign:'center', padding:'2rem', color:'var(--text-lt)' }}>
              No hay productos en el inventario.
            </div>
          ) : (
            <div className="table-wrapper">
              <table>
                <thead>
                  <tr>
                    <th>ID</th><th>Producto ID</th><th>Nombre</th><th>Stock</th><th>Semáforo</th><th>Acciones</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map(i => {
                    const sem = semaforoStock(i.stock)
                    return (
                    <tr key={i.id}>
                      <td><span className="badge badge-blue">{i.id}</span></td>
                      <td><span className="badge badge-gold">{i.productoId}</span></td>
                      <td><strong>{i.nombre}</strong></td>
                      <td>
                        <span className={`badge ${sem.badge}`}>{i.stock}</span>
                      </td>
                      <td>
                        <span className={`badge ${sem.badge}`}>{sem.emoji} {sem.label}</span>
                      </td>
                      <td>
                        <div style={{ display:'flex', gap:'.4rem' }}>
                          <button className="btn btn-outline" onClick={() => abrirEditar(i)}
                            style={{ padding:'.3rem .7rem', fontSize:'.8rem' }}>✏️ Editar</button>
                          <button className="btn btn-danger" onClick={() => handleEliminar(i)}
                            style={{ padding:'.3rem .7rem', fontSize:'.8rem' }}>🗑️</button>
                        </div>
                      </td>
                    </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          )}
        </div>

      </div>
      <footer><p>© 2026 Grupo Cordillera</p></footer>
    </div>
  )
}
