import { useRouter } from 'next/router'
import useSWR from 'swr'

import Loading from '../Loading'
import SectionTitle from '../SectionTitle'
import Value, { VARIANTS } from '../Value'

import JobErrorType from './Type'
import JobErrorTaskMenu from './TaskMenu'

import { formatFullDate } from '../Date/helpers'

import { spacing } from '../Styles'

const JobErrorContent = () => {
  const {
    query: { projectId, errorId },
  } = useRouter()

  const { data: jobError, revalidate } = useSWR(
    `/api/v1/projects/${projectId}/taskerrors/${errorId}`,
  )

  if (typeof jobError === 'undefined') return <Loading />

  const {
    jobName,
    fatal,
    message,
    taskId,
    analyst,
    path,
    processor,
    timeCreated,
  } = jobError

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

      <div
        css={{
          display: 'flex',
          justifyContent: 'flex-start',
          maxWidth: '1440px',
          paddingTop: spacing.normal,
          flexWrap: 'wrap',
        }}>
        <div css={{ paddingRight: spacing.colossal }}>
          <Value legend="Task ID" variant={VARIANTS.SECONDARY}>
            {taskId}
          </Value>
          <Value legend="Error ID" variant={VARIANTS.SECONDARY}>
            {errorId}
          </Value>
          <Value legend="Host ID" variant={VARIANTS.SECONDARY}>
            {analyst}
          </Value>
        </div>
        <div>
          <Value legend="File Path" variant={VARIANTS.SECONDARY}>
            {path}
          </Value>
          <Value legend="Processor" variant={VARIANTS.SECONDARY}>
            {processor}
          </Value>
          <Value legend="Time of Error" variant={VARIANTS.SECONDARY}>
            {formatFullDate({ timestamp: timeCreated })}
          </Value>
        </div>
      </div>
    </>
  )
}

export default JobErrorContent
