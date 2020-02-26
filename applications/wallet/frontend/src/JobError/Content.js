import { useRouter } from 'next/router'
import useSWR from 'swr'

import Loading from '../Loading'
import SectionTitle from '../SectionTitle'

import JobErrorType from './Type'

const JobErrorContent = () => {
  const {
    query: { projectId, errorId },
  } = useRouter()

  const { data: jobError } = useSWR(
    `/api/v1/projects/${projectId}/taskerrors/${errorId}`,
  )

  if (typeof jobError === 'undefined') return <Loading />

  return (
    <>
      <SectionTitle>Job: {jobError.jobName}</SectionTitle>

      <JobErrorType error={jobError} />
    </>
  )
}

export default JobErrorContent
