import express from 'express'
import nextjs from 'next'
import { createProxyMiddleware } from 'http-proxy-middleware'
import morgan from 'morgan'

import user from './src/User/__mocks__/user'
import project from './src/Project/__mocks__/project'
import projects from './src/Projects/__mocks__/projects'
import jobs from './src/Jobs/__mocks__/jobs'
import job from './src/Job/__mocks__/job'
import jobTasks from './src/JobTasks/__mocks__/jobTasks'
import task from './src/Task/__mocks__/task'
import taskErrors from './src/TaskErrors/__mocks__/taskErrors'
import taskError from './src/TaskError/__mocks__/taskError'
import permissions from './src/Permissions/__mocks__/permissions'
import dataSource from './src/DataSource/__mocks__/dataSource'
import dataSources from './src/DataSources/__mocks__/dataSources'
import apiKeys from './src/ApiKeys/__mocks__/apiKeys'
import apiKey from './src/ApiKey/__mocks__/apiKey'
import projectUsers from './src/ProjectUsers/__mocks__/projectUsers'
import projectUser from './src/ProjectUser/__mocks__/projectUser'
import projectUsersAdd from './src/ProjectUsersAdd/__mocks__/projectUsersAdd'
import assets from './src/Assets/__mocks__/assets'
import asset from './src/Asset/__mocks__/asset'
import providers from './src/Providers/__mocks__/providers'
import subscriptions from './src/Subscriptions/__mocks__/subscriptions'
import roles from './src/Roles/__mocks__/roles'
import fields from './src/Filters/__mocks__/fields'
import dateAggregate from './src/FilterDateRange/__mocks__/aggregate'
import models from './src/Models/__mocks__/models'
import modelTypes from './src/ModelTypes/__mocks__/modelTypes'

const { STAGING, SLOW, MOCKED } = process.env

const app = nextjs({ dev: true })
const server = express()
const handle = app.getRequestHandler()
const mock = (response) => (_, res) => res.send(JSON.stringify(response))
const success = () => (_, res) => res.send('{"detail":"Success"}')
const proxy = createProxyMiddleware({
  target: STAGING ? 'https://dev.console.zvi.zorroa.com' : 'http://localhost',
  changeOrigin: true,
  headers: {
    Referer: STAGING
      ? 'https://dev.console.zvi.zorroa.com'
      : 'http://localhost',
  },
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

    server.get('/api/v1/me/', mock(user))
    server.post(`/api/v1/me/agreements/`, success())
    const userpatch = { ...user, firstName: 'David', lastName: 'Smith' }
    server.patch(`/api/v1/me/`, mock(userpatch))

    server.get('/api/v1/projects/', mock(projects))
    server.post('/api/v1/password/reset/', success())
    server.get('/api/v1/projects/:projectId', mock(project))
    server.get(
      '/api/v1/projects/:projectId/task_errors/:errorId/',
      mock(taskError),
    )

    const PID_API_BASE = '/api/v1/projects/:projectId'

    server.get(`${PID_API_BASE}/permissions/`, mock(permissions))

    server.get(`${PID_API_BASE}/jobs/`, mock(jobs))
    server.get(`${PID_API_BASE}/jobs/:jobId/`, mock(job))
    server.get(`${PID_API_BASE}/jobs/:jobId/tasks/`, mock(jobTasks))
    server.get(`${PID_API_BASE}/jobs/:jobId/tasks/:taskId/`, mock(task))
    server.get(`${PID_API_BASE}/jobs/:jobId/errors/`, mock(taskErrors))
    server.get(`${PID_API_BASE}/jobs/:jobId/errors/:errorId/`, mock(taskError))

    server.get(`${PID_API_BASE}/data_sources/:dataSourceId/`, mock(dataSource))
    server.get(`${PID_API_BASE}/data_sources/`, mock(dataSources))
    server.post(`${PID_API_BASE}/data_sources/`, success())

    server.get(`${PID_API_BASE}/api_keys/`, mock(apiKeys))
    server.post(`${PID_API_BASE}/api_keys/`, mock(apiKey))

    server.get(`${PID_API_BASE}/users/`, mock(projectUsers))
    server.get(`${PID_API_BASE}/users/:userId/`, mock(projectUser))
    server.delete(`${PID_API_BASE}/users/:userId/`, success())
    server.patch(`${PID_API_BASE}/users/:userId/`, mock(projectUser))
    server.post(`${PID_API_BASE}/users/`, mock(projectUsersAdd))

    server.get(`${PID_API_BASE}/searches/query/`, mock(assets))
    server.get(`${PID_API_BASE}/assets/:assetId/`, mock(asset))

    server.get(`${PID_API_BASE}/providers/`, mock(providers))
    server.get(`${PID_API_BASE}/subscriptions/`, mock(subscriptions))
    server.get(`${PID_API_BASE}/roles/`, mock(roles))
    server.get(`${PID_API_BASE}/searches/fields/`, mock(fields))
    server.get(`${PID_API_BASE}/searches/aggregate/`, mock(dateAggregate))

    server.get(`${PID_API_BASE}/models/all/`, mock(models))
    server.get(`${PID_API_BASE}/models/model_types/`, mock(modelTypes))
  }

  // Proxy API calls
  server.use('/api', proxy)
  server.use('/auth', proxy)
  server.use('/admin', proxy)
  server.use('/static', proxy)

  server.all('*', (req, res) => handle(req, res))

  server.listen(3000, (err) => {
    if (err) {
      throw err
    }
  })
})
