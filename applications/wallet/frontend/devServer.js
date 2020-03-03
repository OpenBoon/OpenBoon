import express from 'express'
import nextjs from 'next'
import { createProxyMiddleware } from 'http-proxy-middleware'
import morgan from 'morgan'

import user from './src/User/__mocks__/user'
import projects from './src/Projects/__mocks__/projects'
import jobs from './src/Jobs/__mocks__/jobs'
import job from './src/Job/__mocks__/job'
import jobErrors from './src/JobErrors/__mocks__/jobErrors'
import permissions from './src/Permissions/__mocks__/permissions'
import dataSource from './src/DataSource/__mocks__/dataSource'
import dataSources from './src/DataSources/__mocks__/dataSources'
import apiKeys from './src/ApiKeys/__mocks__/apiKeys'
import apiKey from './src/ApiKey/__mocks__/apiKey'
import projectUsers from './src/ProjectUsers/__mocks__/projectUsers'
import projectUser from './src/ProjectUser/__mocks__/projectUser'
import projectUsersAdd from './src/ProjectUsersAdd/__mocks__/projectUsersAdd'

const { STAGING, SLOW, MOCKED } = process.env

const app = nextjs({ dev: true })
const server = express()
const handle = app.getRequestHandler()
const mock = response => (_, res) => res.send(JSON.stringify(response))
const success = () => (_, res) => res.send('{"detail":"Success"}')
const proxy = createProxyMiddleware({
  target: STAGING ? 'https://wallet.zmlp.zorroa.com' : 'http://localhost',
  changeOrigin: true,
})

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
    server.post('/api/v1/password/reset/', success())

    const userpatch = { ...user, firstName: 'David', lastName: 'Smith' }
    server.patch(`/api/v1/users/:userId/`, mock(userpatch))

    const PID_API_BASE = '/api/v1/projects/:projectId'

    server.get(`${PID_API_BASE}/permissions/`, mock(permissions))

    server.get(`${PID_API_BASE}/jobs/`, mock(jobs))
    server.get(`${PID_API_BASE}/jobs/:jobId/`, mock(job))
    server.get(`${PID_API_BASE}/jobs/:jobId/errors`, mock(jobErrors))

    server.get(`${PID_API_BASE}/datasources/:dataSourceId/`, mock(dataSource))
    server.get(`${PID_API_BASE}/datasources/`, mock(dataSources))
    server.post(`${PID_API_BASE}/datasources/`, success())

    server.get(`${PID_API_BASE}/apikeys/`, mock(apiKeys))
    server.post(`${PID_API_BASE}/apikeys/`, mock(apiKey))

    server.get(`${PID_API_BASE}/users/`, mock(projectUsers))
    server.get(`${PID_API_BASE}/users/:userId/`, mock(projectUser))
    server.delete(`${PID_API_BASE}/users/:userId/`, success())
    server.patch(`${PID_API_BASE}/users/:userId/`, mock(projectUser))
    server.post(`${PID_API_BASE}/users/`, mock(projectUsersAdd))
  }

  // Proxy API calls
  server.use('/api', proxy)
  server.use('/auth', proxy)
  server.use('/admin', proxy)
  server.use('/static', proxy)

  server.all('*', (req, res) => handle(req, res))

  server.listen(3000, err => {
    if (err) {
      throw err
    }
  })
})
