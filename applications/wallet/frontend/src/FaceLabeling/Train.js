import PropTypes from 'prop-types'

import { colors, constants, spacing, zIndex } from '../Styles'

import HelpSvg from '../Icons/help.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import { onTrain } from './helpers'

const CONTAINER_HEIGHT = 43

const FaceLabelingTrain = ({
  projectId,
  isTraining,
  showHelpInfo,
  isDisabled,
  dispatch,
}) => {
  return (
    <div css={{ display: 'flex', position: 'relative' }}>
      <Button
        variant={BUTTON_VARIANTS.PRIMARY}
        onClick={() => onTrain({ projectId, dispatch })}
        isDisabled={isDisabled}
      >
        {isTraining ? 'Override Current Training & Re-apply' : 'Train & Apply'}
      </Button>

      {isTraining && (
        <div css={{ display: 'flex' }}>
          <HelpSvg
            aria-label="Training Help"
            role="button"
            tabIndex="0"
            onKeyPress={() => dispatch({ showHelpInfo: !showHelpInfo })}
            onMouseEnter={() => dispatch({ showHelpInfo: true })}
            onMouseLeave={() => dispatch({ showHelpInfo: false })}
            width={20}
            css={{
              color: colors.structure.steel,
              ':hover': { color: colors.structure.white, cursor: 'pointer' },
              marginLeft: spacing.base,
            }}
          />
        </div>
      )}
      {showHelpInfo && (
        <div
          css={{
            position: 'absolute',
            top: CONTAINER_HEIGHT + spacing.small,
            left: 0,
            zIndex: zIndex.reset,
            backgroundColor: colors.structure.iron,
            border: constants.borders.radio,
            borderRadius: constants.borderRadius.small,
            padding: spacing.moderate,
          }}
        >
          Overriding the training that is currently in progress will stop and
          replace it with any new changes made.
        </div>
      )}
    </div>
  )
}

FaceLabelingTrain.propTypes = {
  projectId: PropTypes.string.isRequired,
  isTraining: PropTypes.bool.isRequired,
  showHelpInfo: PropTypes.bool.isRequired,
  isDisabled: PropTypes.bool.isRequired,
  dispatch: PropTypes.func.isRequired,
}

export default FaceLabelingTrain
