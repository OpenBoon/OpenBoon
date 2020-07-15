import PropTypes from 'prop-types'

import { colors, constants, spacing, zIndex } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import HelpSvg from '../Icons/help.svg'

import { onTrain, getHelpInfoCopy } from './helpers'

const CONTAINER_HEIGHT = 43
const ICON_SIZE = 20

const FaceLabelingButton = ({
  projectId,
  jobId,
  unappliedChanges,
  setError,
}) => {
  return (
    <div css={{ display: 'flex', alignItems: 'center', position: 'relative' }}>
      <Button
        variant={BUTTON_VARIANTS.PRIMARY}
        onClick={() => {
          onTrain({ projectId, setError })
        }}
        isDisabled={!unappliedChanges}
      >
        {jobId && unappliedChanges
          ? 'Override Current Training & Re-apply'
          : 'Train & Apply'}
      </Button>

      <div
        css={{
          display: 'flex',
          ':hover, :focus-within': {
            '+ div': {
              visibility: 'visible',
              opacity: 1,
              transition: 'all 0.5s ease-in-out 0.25s',
            },
          },
        }}
      >
        <button
          aria-label="Training Help"
          aria-details="trainingHelpText"
          type="button"
          css={{
            border: 0,
            backgroundColor: 'inherit',
            color: colors.structure.steel,
            ':hover': { color: colors.structure.white, cursor: 'pointer' },
            marginLeft: spacing.base,
          }}
        >
          <HelpSvg height={ICON_SIZE} />
        </button>
      </div>

      <div
        role="tooltip"
        id="trainingHelpText"
        css={{
          position: 'absolute',
          top: CONTAINER_HEIGHT + spacing.small,
          zIndex: zIndex.reset,
          backgroundColor: colors.structure.iron,
          border: constants.borders.regular.white,
          borderRadius: constants.borderRadius.small,
          padding: spacing.moderate,
          color: colors.structure.white,
          userSelect: 'text',
          visibility: 'hidden',
          opacity: 0,
          transition: 'all 0.5s ease 0.25s',
          ':hover, :focus-within': {
            visibility: 'visible',
            opacity: 1,
            transition: 'all 0s',
            cursor: 'text',
          },
        }}
      >
        {getHelpInfoCopy({ jobId, unappliedChanges })}
      </div>
    </div>
  )
}

FaceLabelingButton.propTypes = {
  projectId: PropTypes.string.isRequired,
  jobId: PropTypes.string.isRequired,
  unappliedChanges: PropTypes.bool.isRequired,
  setError: PropTypes.func.isRequired,
}

export default FaceLabelingButton
