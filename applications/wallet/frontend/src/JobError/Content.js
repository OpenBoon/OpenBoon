import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing, typography } from '../Styles'

import Loading from '../Loading'

import JobErrorType from './Type'

const JobErrorContent = () => {
  const {
    query: { projectId, jobId, errorId },
  } = useRouter()

  const { data: job } = useSWR(`/api/v1/projects/${projectId}/jobs/${jobId}`)

  const { data: { results: errors = [] } = {} } = useSWR(
    `/api/v1/projects/${projectId}/jobs/${jobId}/errors/`,
  )
  const currentError = errors.find(err => err.id === errorId)

  if (typeof currentError !== 'object') return <Loading />

  const { name } = job

  return (
    <div>
      <h3
        css={{
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
          paddingTop: spacing.normal,
          paddingBottom: spacing.spacious,
        }}>
        Job: {name}
      </h3>

      <JobErrorType error={currentError} />
    </div>
  )
}

export default JobErrorContent
