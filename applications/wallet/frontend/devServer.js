import express from 'express'
import nextjs from 'next'
import p from 'http-proxy-middleware'
import morgan from 'morgan'

import projects from './src/Projects/__mocks__/projects'
import jobs from './src/Jobs/__mocks__/jobs'
import apikeys from './src/ApiKeys/__mocks__/apikeys'

const { MOCKED, SLOW } = process.env

const app = nextjs({ dev: true })
const server = express()
const handle = app.getRequestHandler()
const mock = response => (_, res) => res.send(JSON.stringify(response))
const proxy = p({ target: 'http://localhost', changeOrigin: true })

app.prepare().then(() => {
  server.use(morgan('combined'))

  // Add 1s delay to every response
  if (SLOW) {
    server.use((req, res, next) => setTimeout(next, 1000))
  }

  // Mock API calls
  if (MOCKED) {
    server.get('/api/v1/projects/', mock(projects))
    server.get('/api/v1/projects/:projectId/jobs/', mock(jobs))
    server.get('/api/v1/projects/:projectId/apikeys/', mock(apikeys))
  }

  // Proxy API calls
  server.use('/api', proxy)
  server.use('/auth', proxy)
  server.use('/admin', proxy)

  server.all('*', (req, res) => handle(req, res))

  server.listen(3000, err => {
    if (err) {
      throw err
    }
  })
})
