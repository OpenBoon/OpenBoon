import PropTypes from 'prop-types'
import useSWR from 'swr'
import { useReducer } from 'react'

import { constants, spacing } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'

import FaceLabelingTrain from './Train'

const INITIAL_STATE = {
  isTraining: false,
  isTrainingSuccess: false,
  showHelpInfo: false,
  error: '',
}

const reducer = (state, action) => ({ ...state, ...action })

const FaceLabelingTrainApply = ({ projectId }) => {
  const {
    data: { unappliedChanges },
  } = useSWR(`/api/v1/projects/${projectId}/faces/unapplied_changes/`)

  const [state, dispatch] = useReducer(reducer, INITIAL_STATE)

  const { isTraining, isTrainingSuccess, showHelpInfo, error } = state

  return (
    <div
      css={{
        padding: spacing.normal,
        borderBottom: constants.borders.divider,
      }}
    >
      {isTraining && (
        <>
          <FlashMessage variant={FLASH_VARIANTS.PROCESSING}>
            Face training in progress. Check status.
          </FlashMessage>
          <div css={{ paddingBottom: spacing.normal }} />
        </>
      )}
      {!!error && (
        <>
          <FlashMessage variant={FLASH_VARIANTS.ERROR}>{error}</FlashMessage>
          <div css={{ paddingBottom: spacing.normal }} />
        </>
      )}
      {isTrainingSuccess && (
        <>
          <FlashMessage variant={FLASH_VARIANTS.SUCCESS}>
            Face training complete.
          </FlashMessage>
          <div css={{ paddingBottom: spacing.normal }} />
        </>
      )}
      <span>
        Once a name has been added to a face, training can begin. Names can
        continue to be edited as needed.
      </span>
      <div css={{ height: spacing.normal }} />

      <FaceLabelingTrain
        projectId={projectId}
        isTraining={isTraining}
        showHelpInfo={showHelpInfo}
        isDisabled={!unappliedChanges}
        dispatch={dispatch}
      />
    </div>
  )
}

FaceLabelingTrainApply.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default FaceLabelingTrainApply
