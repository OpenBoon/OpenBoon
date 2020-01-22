import Head from 'next/head'

import PageTitle from '../PageTitle'

const JobDetails = () => {
  return (
    <>
      <Head>
        <title>Job Details</title>
      </Head>

      <PageTitle>
        <span css={{ color: 'grey' }}>Job Queue /</span> Job Details
      </PageTitle>
    </>
  )
}

export default JobDetails
