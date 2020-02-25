import { useRouter } from 'next/router'
import useSWR from 'swr'

import Loading from '../Loading'
import SectionTitle from '../SectionTitle'
import Value, { VARIANTS } from '../Value'

import JobErrorType from './Type'
import JobErrorJobMenu from './JobMenu'

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

      <JobErrorType fatal={jobError.fatal} />

      <Value legend="Error Message" variant={VARIANTS.SECONDARY}>
        {jobError.message}
      </Value>
    </>
  )
}

export default JobErrorContent
