import Head from 'next/head'
import { useRouter } from 'next/router'

import Breadcrumbs from '../Breadcrumbs'

const JobDetails = () => {
  const {
    query: { jobId },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Job Details</title>
      </Head>

      <Breadcrumbs crumbs={['Job Queue', 'Job Details']} />
      <div>Job ID: {jobId}</div>
    </>
  )
}

export default JobDetails
