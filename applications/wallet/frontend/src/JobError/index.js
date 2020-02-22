import { useRouter } from 'next/router'
import Head from 'next/head'

import Breadcrumbs from '../Breadcrumbs'

const JobError = () => {
  const {
    query: { errorId },
  } = useRouter()

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

      <div>Error ID: {errorId}</div>
    </>
  )
}

export default JobError
