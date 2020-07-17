import PropTypes from 'prop-types'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import Toggletip from '../Toggletip'

import { onTrain, getHelpInfoCopy } from './helpers'

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

      <Toggletip openToThe="left">
        {getHelpInfoCopy({ jobId, unappliedChanges })}
      </Toggletip>
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
