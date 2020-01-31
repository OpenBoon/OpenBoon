import Head from 'next/head'

import Breadcrumbs from '../Breadcrumbs'

import JobErrorsContent from './Content'

const JobErrors = () => {
  return (
    <>
      <Head>
        <title>Task Errors</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Job Queue', href: '/[projectId]/jobs' },
          { title: 'Task Errors', href: false },
        ]}
      />

      <JobErrorsContent />
    </>
  )
}

export default JobErrors
