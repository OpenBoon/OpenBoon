import { useRouter } from 'next/router'
import useSWR from 'swr'

import SectionTitle from '../SectionTitle'
import Value, { VARIANTS } from '../Value'
import Tabs from '../Tabs'

import JobErrorType from '../JobError/Type'
import JobErrorTaskMenu from '../JobError/TaskMenu'
import JobErrorDetails from '../JobError/Details'

const JobErrorAssetContent = () => {
  const {
    query: { projectId, errorId },
  } = useRouter()

  const { data: jobError, revalidate } = useSWR(
    `/api/v1/projects/${projectId}/taskerrors/${errorId}/`,
  )

  const { jobName, fatal, message, taskId } = jobError

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
            href: '/[projectId]/jobs/[jobId]/errors/[errorId]',
          },
          {
            title: 'Asset',
            href: '/[projectId]/jobs/[jobId]/errors/[errorId]/asset',
          },
        ]}
      />

      <div>Asset</div>
    </>
  )
}

export default JobErrorAssetContent
