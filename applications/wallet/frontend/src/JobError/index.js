import Head from 'next/head'

import Breadcrumbs from '../Breadcrumbs'
import SuspenseBoundary from '../SuspenseBoundary'

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
          { title: 'Job Details', href: '/[projectId]/jobs/[jobId]/errors' },
          { title: 'Error Details', href: false },
        ]}
      />

      <SuspenseBoundary>
        <JobErrorContent />
      </SuspenseBoundary>
    </>
  )
}

export default JobError
