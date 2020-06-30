import { useRef, useEffect } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import { constants, spacing } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import FaceLabelingMessage from './Message'

const FaceLabelingTrainApply = ({ projectId }) => {
  const jobIdRef = useRef()

  const {
    data: { unappliedChanges },
  } = useSWR(`/api/v1/projects/${projectId}/faces/unapplied_changes/`)

  const {
    data: { jobId },
  } = useSWR(`/api/v1/projects/${projectId}/faces/training_job/`, {
    refreshInterval: 3000,
  })

  useEffect(() => {
    jobIdRef.current = jobId
  }, [jobId])

  return (
    <div
      css={{
        padding: spacing.normal,
        borderBottom: constants.borders.divider,
      }}
    >
      <FaceLabelingMessage
        projectId={projectId}
        previousJobId={jobIdRef.current}
        currentJobId={jobId}
      />

      <span>
        Once a name has been added to a face, training can begin. Names can
        continue to be edited as needed.
      </span>
      <div css={{ height: spacing.normal }} />
      <Button
        variant={BUTTON_VARIANTS.PRIMARY}
        onClick={console.warn}
        isDisabled={!unappliedChanges}
      >
        {jobId ? 'Override Current Training & Re-apply' : 'Train & Apply'}
      </Button>
    </div>
  )
}

FaceLabelingTrainApply.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default FaceLabelingTrainApply
