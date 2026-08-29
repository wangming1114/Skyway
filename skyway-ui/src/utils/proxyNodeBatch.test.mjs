import assert from 'node:assert/strict'
import { test } from 'node:test'
import { parseSocks5RelayLines, parseSocks5RelayText } from './proxyNodeBatch.js'

test('parses one SOCKS5 relay using the existing four-part format', () => {
  assert.deepEqual(parseSocks5RelayText('204.1.132.8:36772:8XmKfHnr:Rrh5bTVrj3'), {
    ok: true,
    text: '204.1.132.8:36772:8XmKfHnr:Rrh5bTVrj3',
    host: '204.1.132.8',
    port: '36772',
    username: '8XmKfHnr',
    password: 'Rrh5bTVrj3'
  })
})

test('parses non-empty relay lines and preserves invalid lines for row validation', () => {
  const rows = parseSocks5RelayLines(`
204.1.132.8:36772:8XmKfHnr:Rrh5bTVrj3

bad-line
157.238.146.184:36192:VMu70IYA:cCzOATQFnO
`)

  assert.equal(rows.length, 3)
  assert.equal(rows[0].ok, true)
  assert.equal(rows[1].ok, false)
  assert.equal(rows[1].message, '格式应为 host:port:username:password')
  assert.equal(rows[2].host, '157.238.146.184')
})

test('rejects missing credentials and out-of-range ports', () => {
  assert.equal(parseSocks5RelayText('1.2.3.4:1080:user:').ok, false)
  assert.equal(parseSocks5RelayText('1.2.3.4:70000:user:pass').ok, false)
})
