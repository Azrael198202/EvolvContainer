import { useEffect, useRef, useState } from 'react'
import './App.css';

export default function App() {
  const [name, setName] = useState('hello-gpt')
  const [companyId, setCompanyId] = useState("1c5ea0ce-eab3-47f7-a7a0-c20f2fdd0482");
  const [port, setPort] = useState('9080')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [logs, setLogs] = useState([])

  const esRef = useRef(null)

  const stripAnsi = (text) => text.replace(/\u001b\[[0-9;]*m/g, '');

  const closeSSE = () => {
    if (esRef.current) {
      esRef.current.close()
      esRef.current = null
    }
  }

  const pushLog = (line) => setLogs((prev) => [...prev, `[${new Date().toLocaleTimeString()}] ${line}`])

  const callApiWithSSE = async () => {
    setLoading(true)
    setError('')
    setLogs([])

    const id = name.trim() || 'default'
    const company  = companyId.trim() || "default";

    // 1) connect SSE
    try {
      closeSSE()
      const esUrl = `/api/apps/streams/${encodeURIComponent(id)}`
      const es = new EventSource(esUrl)
      esRef.current = es

      es.onopen = () => pushLog(`SSE is connected: ${esUrl}`)
      es.onmessage = (e) => {
        const text = e.data
          .replaceAll('\\n', '\n')
          .replaceAll('\\t', '    ')
        pushLog(stripAnsi(text))
      }
      es.onerror = (e) => {
        pushLog('SSE will be closed')
        pushLog(e.message || JSON.stringify(e))
        es.close()
        esRef.current = null
      }
    } catch (e) {
      setError(`SSE contected is failture: ${e?.message || String(e)}`)
      setLoading(false)
      return
    }

    // 2) call API
    try {
      const url = `/api/apps/template/${encodeURIComponent(name)}?port=${encodeURIComponent(port)}&streamId=${encodeURIComponent(name)}&companyId=${encodeURIComponent(company)}`
      const res = await fetch(url, { method: 'POST' })
      if (!res.ok) {
        const text = await res.text()
        throw new Error(`HTTP ${res.status}: ${text}`)
      }

      const ct = res.headers.get('content-type') || '';
      if (ct.includes('application/json')) {
        const data = await res.json();
        let raw = JSON.stringify(data, null, 2)
          .replace(/\\\\/g, '\\\\')
          .replace(/\\n/g, '\\n')
          .replace(/\\u001b/g, '\\u001b');
        // pushLog("return Result(JSON raw):\n" + raw);
        pushLog("return Result(JSON):\n" + raw);
      } else {
        const text = await res.text();
        pushLog("return Result(Text): " + text);
      }
    } catch (e) {
      setError(e.message || String(e))
    } finally {
      setLoading(false)
    }
  }

  // close SSE on unmount
  useEffect(() => () => closeSSE(), [])

  return (
    <div style={{ maxWidth: 720, margin: '40px', fontFamily: 'system-ui, -apple-system, Segoe UI, Roboto, Helvetica, Arial' }}>
      <h1>Template API + SSE</h1>

      <div style={{ display: 'grid', gap: 12 }}>
        <label>
          Company ID (for container name):
          <input
            value={companyId}
            onChange={(e) => setCompanyId(e.target.value)}
            placeholder="e.g. leadingwin"
            style={{ width: "100%", padding: "8px 10px", border: "1px solid #ddd", borderRadius: 8 }}
          />
        </label>
        <label>
          Name (SSE's id）：
          <input
            placeholder="e.g. hello-gpt"
            value={name}
            onChange={(e) => setName(e.target.value)}
            style={{ width: '100%', padding: '8px 10px', border: '1px solid #ddd', borderRadius: 8 }}
          />
        </label>

        <label>
          Port：
          <input
            placeholder="e.g. 9080"
            value={port}
            onChange={(e) => setPort(e.target.value)}
            style={{ width: '100%', padding: '8px 10px', border: '1px solid #ddd', borderRadius: 8 }}
          />
        </label>

        <button
          onClick={callApiWithSSE}
          disabled={loading || !name}
          style={{
            padding: '10px 14px',
            borderRadius: 10,
            border: '1px solid #333',
            cursor: loading || !name ? 'not-allowed' : 'pointer',
            background: loading || !name ? '#e5e7eb' : '#111',
            color: loading || !name ? '#111' : '#fff',
            fontWeight: 600,
          }}
        >
          {loading ? 'Request…' : 'call API and listen SSE'}
        </button>

        <div>
          <div style={{ fontWeight: 700, marginBottom: 6 }}>Output and message</div>
          <div className="bg-black text-white p-4 rounded-xl">
            <pre className="whitespace-pre-wrap font-mono log-output">{logs.join('\n')}</pre>
          </div>
        </div>
      </div>
    </div>
  )
}