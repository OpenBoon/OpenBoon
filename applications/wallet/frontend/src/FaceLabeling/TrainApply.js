import PropTypes from 'prop-types'
import useSWR from 'swr'

import { constants, spacing } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

const FaceLabelingTrainApply = ({ projectId }) => {
  const {
    data: { unappliedChanges },
  } = useSWR(`/api/v1/projects/${projectId}/faces/unapplied_changes/`)

  return (
    <div
      css={{
        padding: spacing.normal,
        borderBottom: constants.borders.divider,
      }}
    >
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
        Train &amp; Apply
      </Button>
    </div>
  )
}

FaceLabelingTrainApply.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default FaceLabelingTrainApply
