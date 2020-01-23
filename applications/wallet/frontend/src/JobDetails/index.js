import Head from 'next/head'
import { useRouter } from 'next/router'

import PageTitle from '../PageTitle'

const JobDetails = () => {
  const {
    query: { jobId },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Job Details</title>
      </Head>

      <PageTitle>Job Queue / Job Details</PageTitle>

      <div>Job ID: {jobId}</div>
    </>
  )
}

export default JobDetails
