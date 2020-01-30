import Head from 'next/head'
import { useRouter } from 'next/router'

import Breadcrumbs from '../Breadcrumbs'
import Tabs from '../Tabs'
import Table from '../Table'

import JobErrorsEmpty from './Empty'
import JobErrorsRow from './Row'

const JobErrors = () => {
  const {
    query: { projectId, jobId },
  } = useRouter()

  return (
    <>
      <Head>
        <title>Job Queue</title>
      </Head>

      <Breadcrumbs
        crumbs={[
          { title: 'Job Queue', href: '/[projectId]/jobs' },
          { title: 'Task Errors', href: false },
        ]}
      />

      <Tabs
        tabs={[
          { title: 'All Errors', href: '/[projectId]/jobs/[jobId]/errors' },
        ]}
      />

      <Table
        url={`/api/v1/projects/${projectId}/jobs/${jobId}/errors`}
        columns={[
          'Error Type',
          'Error ID',
          'Task ID',
          'Message',
          'File Path',
          'Processor',
          'Time',
          '#Actions#',
        ]}
        expandColumn={4}
        renderEmpty={<JobErrorsEmpty />}
        renderRow={({ result, revalidate }) => (
          <JobErrorsRow
            key={result.id}
            projectId={projectId}
            jobId={jobId}
            error={result}
            revalidate={revalidate}
          />
        )}
      />
    </>
  )
}

export default JobErrors
