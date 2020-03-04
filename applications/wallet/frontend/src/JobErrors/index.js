import Head from 'next/head'
import { useRouter } from 'next/router'

import Breadcrumbs from '../Breadcrumbs'
import SuspenseBoundary from '../SuspenseBoundary'
import Tabs from '../Tabs'
import Table from '../Table'

import JobErrorsContent from './Content'
import JobErrorsEmpty from './Empty'
import JobErrorsRow from './Row'

const JobErrors = () => {
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
          { title: 'Job Queue', href: '/[projectId]/jobs' },
          { title: 'Job Details', href: false },
        ]}
      />

      <SuspenseBoundary>
        <JobErrorsContent />

        <Tabs
          tabs={[{ title: 'Errors', href: '/[projectId]/jobs/[jobId]/errors' }]}
        />

        <Table
          url={`/api/v1/projects/${projectId}/jobs/${jobId}/errors`}
          columns={[
            'Error Type',
            'Phase',
            'Message',
            'File Name',
            'Processor',
            'Time',
            '#Actions#',
          ]}
          expandColumn={0}
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
      </SuspenseBoundary>
    </>
  )
}

export default JobErrors
