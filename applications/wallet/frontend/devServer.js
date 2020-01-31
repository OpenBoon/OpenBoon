import express from 'express'
import nextjs from 'next'
import p from 'http-proxy-middleware'
import morgan from 'morgan'

import user from './src/User/__mocks__/user'
import projects from './src/Projects/__mocks__/projects'
import jobs from './src/Jobs/__mocks__/jobs'
import job from './src/Job/__mocks__/job'
import jobErrors from './src/JobErrors/__mocks__/jobErrors'
import permissions from './src/Permissions/__mocks__/permissions'
import apikeys from './src/ApiKeys/__mocks__/apikeys'
import apikeysadd from './src/ApiKeysAdd/__mocks__/apikeysadd'
import projectUsers from './src/ProjectUsers/__mocks__/projectUsers'

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
    server.post('/api/v1/login/', mock(user))
    server.get('/api/v1/projects/', mock(projects))

    const PID_API_BASE = '/api/v1/projects/:projectId'

    server.get(`${PID_API_BASE}/jobs/`, mock(jobs))
    server.get(`${PID_API_BASE}/permissions/`, mock(permissions))
    server.get(`${PID_API_BASE}/jobs/:jobId/`, mock(job))
    server.get(`${PID_API_BASE}/jobs/:jobId/errors`, mock(jobErrors))
    server.get(`${PID_API_BASE}/apikeys/`, mock(apikeys))
    server.post(`${PID_API_BASE}/apikeys/`, mock(apikeysadd))
    server.get(`${PID_API_BASE}/users/`, mock(projectUsers))
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
