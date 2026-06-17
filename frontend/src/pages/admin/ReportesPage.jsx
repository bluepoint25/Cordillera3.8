import { useState, useEffect } from 'react'
import Navbar from '../../components/Navbar'
import { reportesApi } from '../../api/apiClient'
import { useAuth } from '../../context/AuthContext'

const TIPOS = ['KPI', 'MENSUAL', 'RESUMEN']
const TIPO_INFO = {
  KPI:     { icon: '📊', color: '#1B4F72', desc: 'Indicadores de KPI del periodo' },
  MENSUAL: { icon: '📅', color: '#1E8449', desc: 'Informe mensual completo'        },
  RESUMEN: { icon: '📋', color: '#8E44AD', desc: 'Resumen ejecutivo consolidado'   },
}

function descargarCSV(reportes) {
  const encabezado = ['ID', 'Tipo', 'Título', 'Periodo', 'Estado', 'Generado por', 'Fecha', 'Contenido']
  const filas = reportes.map(r => [
    r.id,
    r.tipo,
    `"${(r.titulo || '').replace(/"/g, '""')}"`,
    r.periodo,
    r.estado,
    `"${(r.generadoPor || '').replace(/"/g, '""')}"`,
    r.createdAt ? r.createdAt.split('T')[0] : '',
    `"${(r.contenido || '').replace(/"/g, '""').replace(/\n/g, ' ')}"`,
  ])
  const csv = [encabezado.join(','), ...filas.map(f => f.join(','))].join('\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `reportes_cordillera_${new Date().toISOString().slice(0,10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

function descargarPDF(reporte) {
  const fecha = reporte.createdAt ? reporte.createdAt.split('T')[0] : ''
  const html = `<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>${reporte.titulo}</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    body { font-family: 'Segoe UI', Arial, sans-serif; color: #1a1a2e; padding: 40px; font-size: 13px; }
    .header { border-bottom: 3px solid #1B4F72; padding-bottom: 16px; margin-bottom: 24px; }
    .logo { font-size: 22px; font-weight: 800; color: #1B4F72; letter-spacing: -0.5px; }
    .logo span { color: #2980B9; }
    h1 { font-size: 18px; margin-top: 10px; color: #1a1a2e; }
    .meta { display: flex; gap: 20px; margin: 16px 0; flex-wrap: wrap; }
    .badge { background: #EBF5FB; color: #1B4F72; border-radius: 4px; padding: 3px 10px; font-size: 11px; font-weight: 700; }
    .badge.verde { background: #EAFAF1; color: #1E8449; }
    .badge.dorado { background: #FEFBD0; color: #7D6608; }
    .content-box { background: #F8F9FA; border: 1px solid #ddd; border-radius: 6px; padding: 20px; margin-top: 20px; white-space: pre-wrap; font-family: 'Courier New', monospace; font-size: 12px; line-height: 1.6; }
    .footer { margin-top: 32px; border-top: 1px solid #ddd; padding-top: 12px; font-size: 11px; color: #888; display: flex; justify-content: space-between; }
    @media print {
      body { padding: 20px; }
      .no-print { display: none; }
    }
  </style>
</head>
<body>
  <div class="header">
    <div class="logo">Cordillera <span>Analytics</span></div>
    <h1>${reporte.titulo}</h1>
  </div>
  <div class="meta">
    <span class="badge">${TIPO_INFO[reporte.tipo]?.icon || '📄'} ${reporte.tipo}</span>
    <span class="badge verde">📅 Periodo: ${reporte.periodo}</span>
    <span class="badge dorado">Estado: ${reporte.estado}</span>
  </div>
  <p><strong>Generado por:</strong> ${reporte.generadoPor || '—'} &nbsp;|&nbsp; <strong>Fecha:</strong> ${fecha}</p>
  <div class="content-box">${(reporte.contenido || 'Sin contenido').replace(/</g, '&lt;').replace(/>/g, '&gt;')}</div>
  <div class="footer">
    <span>Reporte #${reporte.id} — Sistema Cordillera</span>
    <span>Generado el ${new Date().toLocaleDateString('es-CL')}</span>
  </div>
  <script>window.onload = () => { window.print(); }<\/script>
</body>
</html>`

  const ventana = window.open('', '_blank', 'width=800,height=700')
  ventana.document.write(html)
  ventana.document.close()
}

export default function ReportesPage() {
  const { user } = useAuth()
  const [reportes, setReportes] = useState([])
  const [loading, setLoading]   = useState(true)
  const [error, setError]       = useState('')
  const [success, setSuccess]   = useState('')
  const [showForm, setShowForm] = useState(false)
  const [selected, setSelected] = useState(null)
  const [form, setForm] = useState({ tipo: 'RESUMEN', titulo: '', periodo: '', descripcionAdicional: '' })
  const [saving, setSaving] = useState(false)

  useEffect(() => { cargarReportes() }, [])

  const cargarReportes = async () => {
    try {
      setLoading(true)
      const res = await reportesApi.listar()
      setReportes(Array.isArray(res.data) ? res.data : [])
    } catch { setError('No se pudo conectar con el servicio de reportes') }
    finally { setLoading(false) }
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setSaving(true); setError(''); setSuccess('')
    try {
      await reportesApi.generar({ ...form, generadoPor: user?.nombre })
      setSuccess('✅ Reporte generado exitosamente')
      setShowForm(false)
      setForm({ tipo: 'RESUMEN', titulo: '', periodo: '', descripcionAdicional: '' })
      cargarReportes()
    } catch (err) {
      setError(err.response?.data?.message || 'Error al generar el reporte')
    } finally { setSaving(false) }
  }

  return (
    <div className="page-wrapper">
      <Navbar />
      <div className="page-content">
        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'flex-start', marginBottom:'1.5rem' }}>
          <div>
            <h1 className="page-title">📋 Reportes</h1>
            <p className="page-subtitle">Genera y visualiza reportes empresariales</p>
          </div>
          <div style={{ display:'flex', gap:'.75rem' }}>
            {reportes.length > 0 && (
              <button className="btn btn-outline" onClick={() => descargarCSV(reportes)}
                title="Descargar todos los reportes como CSV"
                style={{ display:'flex', alignItems:'center', gap:'.4rem' }}>
                📥 CSV
              </button>
            )}
            <button className="btn btn-primary" onClick={() => { setShowForm(!showForm); setSelected(null) }}>
              {showForm ? '✕ Cancelar' : '+ Nuevo Reporte'}
            </button>
          </div>
        </div>

        {error   && <div className="alert alert-error">{error}</div>}
        {success && <div className="alert alert-success">{success}</div>}

        {/* Tipos de reporte */}
        {showForm && (
          <div className="card" style={{ marginBottom:'1.5rem' }}>
            <div className="card-header"><span className="card-title">Generar nuevo reporte</span></div>

            <div className="grid-3" style={{ marginBottom:'1.5rem' }}>
              {TIPOS.map(t => (
                <div key={t}
                  onClick={() => setForm({...form, tipo: t})}
                  style={{
                    border: `2px solid ${form.tipo === t ? TIPO_INFO[t].color : 'var(--border)'}`,
                    borderRadius: 10, padding:'1rem', cursor:'pointer', textAlign:'center',
                    background: form.tipo === t ? TIPO_INFO[t].color + '10' : '#fff',
                    transition: 'all .2s',
                  }}>
                  <div style={{ fontSize:'2rem' }}>{TIPO_INFO[t].icon}</div>
                  <div style={{ fontWeight:700, color: form.tipo === t ? TIPO_INFO[t].color : 'var(--text)', marginTop:'.4rem' }}>{t}</div>
                  <div style={{ fontSize:'.8rem', color:'var(--text-lt)', marginTop:'.25rem' }}>{TIPO_INFO[t].desc}</div>
                </div>
              ))}
            </div>

            <form onSubmit={handleSubmit}>
              <div className="form-row">
                <div className="form-group">
                  <label className="form-label">Título del reporte</label>
                  <input className="form-input" placeholder="Ej: Informe Ejecutivo Mayo 2026"
                    value={form.titulo} required
                    onChange={e => setForm({...form, titulo: e.target.value})} />
                </div>
                <div className="form-group">
                  <label className="form-label">Periodo (YYYY-MM)</label>
                  <input className="form-input" placeholder="2026-05"
                    value={form.periodo} required pattern="\d{4}-\d{2}"
                    onChange={e => setForm({...form, periodo: e.target.value})} />
                </div>
              </div>
              <div className="form-group">
                <label className="form-label">Observaciones adicionales</label>
                <textarea className="form-input" style={{ height:'80px', resize:'vertical' }}
                  placeholder="Comentarios o conclusiones adicionales..."
                  value={form.descripcionAdicional}
                  onChange={e => setForm({...form, descripcionAdicional: e.target.value})} />
              </div>
              <div className="flex-end">
                <button type="submit" className="btn btn-primary" disabled={saving}>
                  {saving ? 'Generando...' : `${TIPO_INFO[form.tipo].icon} Generar reporte ${form.tipo}`}
                </button>
              </div>
            </form>
          </div>
        )}

        <div className="grid-2" style={{ alignItems:'flex-start' }}>
          {/* Lista */}
          <div className="card">
            <div className="card-header">
              <span className="card-title">Reportes generados ({reportes.length})</span>
              <button className="btn btn-outline" onClick={cargarReportes} style={{ padding:'.4rem .8rem', fontSize:'.85rem' }}>
                🔄
              </button>
            </div>
            {loading ? <div className="loading">Cargando...</div> : (
              reportes.length === 0
                ? <div style={{ textAlign:'center', padding:'2rem', color:'var(--text-lt)' }}>No hay reportes aún</div>
                : <div style={{ display:'flex', flexDirection:'column', gap:'.5rem' }}>
                    {reportes.map(r => (
                      <div key={r.id}
                        onClick={() => setSelected(r)}
                        style={{
                          padding:'1rem', borderRadius:8, cursor:'pointer',
                          border: `1.5px solid ${selected?.id === r.id ? 'var(--primary)' : 'var(--border)'}`,
                          background: selected?.id === r.id ? '#EBF5FB' : '#fff',
                          transition:'all .15s',
                        }}>
                        <div style={{ display:'flex', justifyContent:'space-between', alignItems:'center' }}>
                          <span style={{ fontWeight:700, fontSize:'.9rem' }}>{TIPO_INFO[r.tipo]?.icon || '📄'} {r.titulo}</span>
                          <span className={`badge badge-${r.tipo === 'KPI' ? 'blue' : r.tipo === 'MENSUAL' ? 'green' : 'gold'}`}>
                            {r.tipo}
                          </span>
                        </div>
                        <div style={{ fontSize:'.8rem', color:'var(--text-lt)', marginTop:'.3rem' }}>
                          📅 {r.periodo} · 👤 {r.generadoPor}
                        </div>
                      </div>
                    ))}
                  </div>
            )}
          </div>

          {/* Detalle */}
          <div className="card">
            <div className="card-header">
              <span className="card-title">Contenido del reporte</span>
              {selected && (
                <div style={{ display:'flex', gap:'.5rem' }}>
                  <button
                    className="btn btn-outline"
                    onClick={() => descargarCSV([selected])}
                    title="Descargar este reporte como CSV"
                    style={{ padding:'.35rem .75rem', fontSize:'.8rem' }}>
                    📥 CSV
                  </button>
                  <button
                    className="btn btn-primary"
                    onClick={() => descargarPDF(selected)}
                    title="Descargar este reporte como PDF"
                    style={{ padding:'.35rem .75rem', fontSize:'.8rem' }}>
                    📄 PDF
                  </button>
                </div>
              )}
            </div>
            {selected ? (
              <div>
                <h3 style={{ color:'var(--primary)', marginBottom:'.5rem' }}>{selected.titulo}</h3>
                <div style={{ marginBottom:'.75rem', display:'flex', gap:'.5rem', flexWrap:'wrap' }}>
                  <span className="badge badge-blue">{selected.tipo}</span>
                  <span className="badge badge-green">{selected.periodo}</span>
                  <span className="badge badge-gold">{selected.estado}</span>
                </div>

                {/* Leyenda del rango de KPIs (PROMEDIO / SUMA) */}
                <div style={{
                  display:'flex', gap:'1rem', flexWrap:'wrap', alignItems:'center',
                  background:'#F8F9FA', border:'1px solid var(--border)', borderRadius:8,
                  padding:'.5rem .75rem', marginBottom:'.75rem', fontSize:'.8rem',
                }}>
                  <strong style={{ color:'var(--text-lt)' }}>Rango KPI (promedio/suma):</strong>
                  <span>🟢 Óptimo</span>
                  <span>🟡 Regular</span>
                  <span>🔴 Malo</span>
                </div>
                <pre style={{
                  background:'#F8F9FA', borderRadius:8, padding:'1rem',
                  fontFamily:'monospace', fontSize:'.82rem', whiteSpace:'pre-wrap',
                  maxHeight:360, overflowY:'auto', color:'var(--text)',
                  border:'1px solid var(--border)',
                }}>
                  {selected.contenido || 'Sin contenido'}
                </pre>
                <p style={{ fontSize:'.8rem', color:'var(--text-lt)', marginTop:'.5rem' }}>
                  Generado por: {selected.generadoPor} · {selected.createdAt?.split('T')[0]}
                </p>
              </div>
            ) : (
              <div style={{ textAlign:'center', padding:'2rem', color:'var(--text-lt)' }}>
                <div style={{ fontSize:'3rem', marginBottom:'1rem' }}>📋</div>
                Selecciona un reporte para ver su contenido
              </div>
            )}
          </div>
        </div>
      </div>
      <footer><p>© 2026 Grupo Cordillera</p></footer>
    </div>
  )
}
