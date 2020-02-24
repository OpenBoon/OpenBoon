import Head from 'next/head'

import Breadcrumbs from '../Breadcrumbs'

import JobErrorsContent from './Content'

const JobErrors = () => {
  return (
    <>
      <Head>
        <title>Job Details</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Job Queue', href: '/[projectId]/jobs' },
          { title: 'Job Details', href: false },
        ]}
      />

      <JobErrorsContent />
    </>
  )
}

export default JobErrors
