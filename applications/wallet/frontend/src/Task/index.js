import Head from 'next/head'
import { useRouter } from 'next/router'

import Breadcrumbs from '../Breadcrumbs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'
import Tabs from '../Tabs'

import TaskDetails from './Details'

const TASK_URL = '/[projectId]/jobs/[jobId]/tasks/[taskId]'

const Task = () => {
  const { pathname } = useRouter()

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

        <Tabs tabs={[{ title: 'Assets', href: `${TASK_URL}/assets` }]} />

        {pathname === TASK_URL && 'Log'}

        {pathname === `${TASK_URL}/details` && 'Details'}

        {pathname === `${TASK_URL}/assets` && 'Assets'}

        {pathname === `${TASK_URL}/errors` && 'Errors'}
      </SuspenseBoundary>
    </>
  )
}

export default Task
