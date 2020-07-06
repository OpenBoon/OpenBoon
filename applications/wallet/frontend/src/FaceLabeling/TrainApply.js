import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import { constants, spacing } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import FaceLabelingMessage from './Message'

import { onTrain } from './helpers'

const FaceLabelingTrainApply = ({ projectId }) => {
  const {
    data: { jobId, unappliedChanges },
  } = useSWR(`/api/v1/projects/${projectId}/faces/status/`, {
    refreshInterval: 3000,
  })

  const [previousJobId, setPreviousJobId] = useState(jobId)

  const [error, setError] = useState('')

  return (
    <div
      css={{
        padding: spacing.normal,
        borderBottom: constants.borders.divider,
      }}
    >
      <FaceLabelingMessage
        projectId={projectId}
        previousJobId={previousJobId}
        jobId={jobId}
        error={error}
        setPreviousJobId={setPreviousJobId}
      />

      <span>
        Once a name has been added to a face, training can begin. Names can
        continue to be edited as needed.
      </span>

      <div css={{ height: spacing.normal }} />

      <Button
        variant={BUTTON_VARIANTS.PRIMARY}
        onClick={() => onTrain({ projectId, setError })}
        isDisabled={!unappliedChanges}
      >
        {jobId && unappliedChanges
          ? 'Override Current Training & Re-apply'
          : 'Train & Apply'}
      </Button>
    </div>
  )
}

FaceLabelingTrainApply.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default FaceLabelingTrainApply
