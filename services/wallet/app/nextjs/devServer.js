import express from 'express'
import nextjs from 'next'
import proxy from 'http-proxy-middleware'
import morgan from 'morgan'

const app = nextjs({ dev: true })
const server = express()
const handle = app.getRequestHandler()

app.prepare().then(() => {
  server.use(morgan('combined'))

  server.use('/api', proxy({ target: 'http://localhost', changeOrigin: true }))
  server.use('/auth', proxy({ target: 'http://localhost', changeOrigin: true }))
  server.use(
    '/admin',
    proxy({ target: 'http://localhost', changeOrigin: true }),
  )
  server.use(
    '/wallet',
    proxy({ target: 'http://localhost', changeOrigin: true }),
  )

  server.all('*', (req, res) => handle(req, res))

  server.listen(3000, err => {
    if (err) {
      throw err
    }
  })
})
