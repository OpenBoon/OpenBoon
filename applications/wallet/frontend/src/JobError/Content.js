import { useRouter } from 'next/router'
import useSWR from 'swr'

import Loading from '../Loading'
import SectionTitle from '../SectionTitle'
import Value, { VARIANTS } from '../Value'

import JobErrorType from './Type'

import { colors, spacing } from '../Styles'

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

      <div css={{ paddingTop: spacing.normal }}>
        <Value legend="Error Message" variant={VARIANTS.SECONDARY}>
          <div css={{ color: colors.structure.white }}>{jobError.message}</div>
        </Value>
      </div>
    </>
  )
}

export default JobErrorContent
