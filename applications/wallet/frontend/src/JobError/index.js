import Head from 'next/head'

import Breadcrumbs from '../Breadcrumbs'
import SuspenseBoundary, { ROLES } from '../SuspenseBoundary'

import JobErrorContent from './Content'

const JobError = () => {
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
            href: '/[projectId]/jobs/[jobId]/tasks/[taskId]/assets',
          },
          { title: 'Error Details', href: false },
        ]}
      />

      <SuspenseBoundary role={ROLES.ML_Tools}>
        <JobErrorContent />
      </SuspenseBoundary>
    </>
  )
}

export default JobError
