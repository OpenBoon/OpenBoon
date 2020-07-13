import { useState } from 'react'
import PropTypes from 'prop-types'
import useSWR from 'swr'

import { constants, spacing } from '../Styles'

import FaceLabelingMessage from './Message'
import FaceLabelingButton from './Button'

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
        borderBottom: constants.borders.regular.smoke,
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

      <FaceLabelingButton
        key={jobId}
        projectId={projectId}
        jobId={jobId}
        unappliedChanges={unappliedChanges}
        setError={setError}
      />
    </div>
  )
}

FaceLabelingTrainApply.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default FaceLabelingTrainApply
