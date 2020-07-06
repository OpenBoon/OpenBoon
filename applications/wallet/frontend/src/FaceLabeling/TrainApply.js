import { useRef, useEffect, useState } from 'react'
import PropTypes from 'prop-types'
import useSWR, { cache } from 'swr'

import { constants, spacing } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import FaceLabelingMessage from './Message'

import { onTrain } from './helpers'

const FaceLabelingTrainApply = ({ projectId }) => {
  const jobIdRef = useRef()

  const {
    data: { jobId, unappliedChanges },
  } = useSWR(`/api/v1/projects/${projectId}/faces/status/`, {
    refreshInterval: 3000,
  })

  useEffect(() => {
    if (!!jobIdRef.current && !jobId) {
      cache
        .keys()
        .filter((key) => key.includes('/faces'))
        .forEach((key) => cache.delete(key))
    }
    jobIdRef.current = jobId
  }, [jobId])

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
        previousJobId={jobIdRef.current || ''}
        currentJobId={jobId}
        error={error}
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
        {jobId ? 'Override Current Training & Re-apply' : 'Train & Apply'}
      </Button>
    </div>
  )
}

FaceLabelingTrainApply.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default FaceLabelingTrainApply
