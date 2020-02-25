import { useRouter } from 'next/router'
import useSWR from 'swr'

import { spacing } from '../Styles'

import Loading from '../Loading'
import SectionTitle from '../SectionTitle'

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

  if (typeof currentError === 'undefined' || job.id !== jobId)
    return <Loading />

  const { name } = job

  return (
    <div>
      <div css={{ paddingBottom: spacing.spacious }}>
        <SectionTitle>Job: {name}</SectionTitle>
      </div>

      <JobErrorType error={currentError} />
    </div>
  )
}

export default JobErrorContent
