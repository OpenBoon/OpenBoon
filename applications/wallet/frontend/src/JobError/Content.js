import { useRouter } from 'next/router'
import useSWR from 'swr'

import SectionTitle from '../SectionTitle'
import Value, { VARIANTS } from '../Value'
import Tabs from '../Tabs'
import JobErrorStackTrace from '../JobErrorStackTrace'
import JobErrorAsset from '../JobErrorAsset'
import SuspenseBoundary from '../SuspenseBoundary'

import JobErrorType from './Type'
import JobErrorTaskMenu from './TaskMenu'
import JobErrorDetails from './Details'

const JobErrorContent = () => {
  const {
    pathname,
    query: { projectId, errorId },
  } = useRouter()

  const { data: jobError, revalidate } = useSWR(
    `/api/v1/projects/${projectId}/task_errors/${errorId}/`,
  )

  const { jobName, fatal, message, taskId, assetId } = jobError

  return (
    <>
      <SectionTitle>Job: {jobName}</SectionTitle>

      <JobErrorType fatal={fatal} />

      <Value legend="Error Message" variant={VARIANTS.SECONDARY}>
        {message}
      </Value>

      <JobErrorTaskMenu
        projectId={projectId}
        taskId={taskId}
        revalidate={revalidate}
      />

      <JobErrorDetails jobError={jobError} />

      <Tabs
        tabs={[
          {
            title: 'Stack Trace',
            href: '/[projectId]/jobs/[jobId]/tasks/[taskId]/errors/[errorId]',
          },
          {
            title: 'Asset',
            href:
              '/[projectId]/jobs/[jobId]/tasks/[taskId]/errors/[errorId]/asset',
          },
        ]}
      />

      {pathname ===
        '/[projectId]/jobs/[jobId]/tasks/[taskId]/errors/[errorId]' && (
        <JobErrorStackTrace jobError={jobError} />
      )}

      {pathname ===
        '/[projectId]/jobs/[jobId]/tasks/[taskId]/errors/[errorId]/asset' && (
        <SuspenseBoundary>
          <JobErrorAsset assetId={assetId} />
        </SuspenseBoundary>
      )}
    </>
  )
}

export default JobErrorContent
