import { useRouter } from 'next/router'
import Head from 'next/head'

import Breadcrumbs from '../Breadcrumbs'

const TaskErrors = () => {
  const {
    query: { taskId },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Task Errors</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Job Queue', href: '/[projectId]/jobs' },
          { title: 'Task Errors', href: '/[projectId]/jobs/[jobId]/errors' },
          { title: 'ErrorDetails', href: false },
        ]}
      />

      <div>Errors for task: {taskId}</div>
    </>
  )
}

export default TaskErrors
