import Head from 'next/head'
import { useRouter } from 'next/router'

import Breadcrumbs from '../Breadcrumbs'

const JobDetails = () => {
  const {
    query: { projectId, jobId },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Job Details</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Job Queue', href: `/${projectId}/jobs` },
          { title: 'Job Details', href: `/${projectId}/jobs/${jobId}` },
        ]}
      />
      <div>Job ID: {jobId}</div>
    </>
  )
}

export default JobDetails
