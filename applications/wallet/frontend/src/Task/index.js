import Head from 'next/head'
import { useRouter } from 'next/router'

import Breadcrumbs from '../Breadcrumbs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'
import Tabs from '../Tabs'
import TaskScript from '../TaskScript'
import TaskAssets from '../TaskAssets'
import TaskErrors from '../TaskErrors'
import TaskLogs from '../TaskLogs'

import TaskDetails from './Details'

const TASK_URL = '/[projectId]/jobs/[jobId]/tasks/[taskId]'

const Task = () => {
  const {
    pathname,
    query: { projectId, taskId, refreshParam },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Task Details</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Job Queue', href: '/[projectId]/jobs' },
          { title: 'Job Details', href: '/[projectId]/jobs/[jobId]' },
          { title: 'Task Details', href: false },
        ]}
      />

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <TaskDetails key={pathname} />

        <Tabs
          tabs={[
            { title: 'Script', href: `${TASK_URL}` },
            { title: 'Assets', href: `${TASK_URL}/assets` },
            { title: 'Errors', href: `${TASK_URL}/errors` },
            { title: 'Logs', href: `${TASK_URL}/logs` },
          ]}
        />

        {pathname === `${TASK_URL}` && (
          <SuspenseBoundary>
            <TaskScript />
          </SuspenseBoundary>
        )}

        {pathname === `${TASK_URL}/assets` && (
          <SuspenseBoundary>
            <TaskAssets />
          </SuspenseBoundary>
        )}

        {pathname === `${TASK_URL}/errors` && (
          <TaskErrors
            key={refreshParam}
            parentUrl={`/api/v1/projects/${projectId}/tasks/${taskId}/`}
          />
        )}

        {pathname === `${TASK_URL}/logs` && (
          <SuspenseBoundary>
            <TaskLogs />
          </SuspenseBoundary>
        )}
      </SuspenseBoundary>
    </>
  )
}

export default Task
