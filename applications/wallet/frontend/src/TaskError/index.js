import Head from 'next/head'

import Breadcrumbs from '../Breadcrumbs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import TaskErrorContent from './Content'

const TaskError = () => {
  return (
    <>
      <Head>
        <title>Error Details</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Job Queue', href: '/[projectId]/jobs' },
          { title: 'Job Details', href: '/[projectId]/jobs/[jobId]' },
          {
            title: 'Task Details',
            href: '/[projectId]/jobs/[jobId]/tasks/[taskId]',
          },
          { title: 'Error Details', href: false },
        ]}
      />

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <TaskErrorContent />
      </SuspenseBoundary>
    </>
  )
}

export default TaskError
