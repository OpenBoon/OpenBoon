import Head from 'next/head'

import Breadcrumbs from '../Breadcrumbs'
import JobErrorAssetContent from './Content'

const JobErrorAsset = () => {
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

      <JobErrorAssetContent />
    </>
  )
}

export default JobErrorAsset
