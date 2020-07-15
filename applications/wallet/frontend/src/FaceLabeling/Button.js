import PropTypes from 'prop-types'

import { colors, spacing } from '../Styles'

import HelpSvg from '../Icons/help.svg'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Tooltip from '../Tooltip'

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

      <Tooltip
        content={getHelpInfoCopy({ jobId, unappliedChanges })}
        style={{ top: CONTAINER_HEIGHT + spacing.small, left: 0 }}
      >
        <button
          aria-label="Training Help"
          type="button"
          css={{
            border: 0,
            backgroundColor: 'inherit',
            color: colors.structure.steel,
            ':hover': {
              color: colors.structure.white,
              cursor: 'pointer',
            },
            marginLeft: spacing.base,
          }}
        >
          <HelpSvg height={ICON_SIZE} />
        </button>
      </Tooltip>
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
