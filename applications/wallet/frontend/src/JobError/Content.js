import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

import Loading from '../Loading'
import SectionTitle from '../SectionTitle'

import JobErrorType from './Type'

const JobErrorContent = () => {
  const {
    query: { projectId, errorId },
  } = useRouter()

  const { data: taskError } = useSWR(
    `/api/v1/projects/${projectId}/taskerrors/${errorId}`,
  )

  if (typeof taskError === 'undefined') return <Loading />

  return (
    <div>
      <div css={{ paddingBottom: spacing.spacious }}>
        <SectionTitle>Job: {taskError.jobName}</SectionTitle>
      </div>

      <JobErrorType error={taskError} />
    </div>
  )
}

export default JobErrorContent
