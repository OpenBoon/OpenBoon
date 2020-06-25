import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

import FaceLabelingForm from './Form'

const FaceLabelingLabels = ({ projectId, assetId, predictions }) => {
  const { data: asset } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/`,
  )

  const {
    metadata: {
      source: { filename },
    },
  } = asset

  const {
    data: { unappliedChanges },
  } = useSWR(`/api/v1/projects/${projectId}/faces/unapplied_changes/`)

  return (
    <>
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

      <div
        css={{
          padding: spacing.normal,
          color: colors.signal.sky.base,
        }}
      >
        {filename}
      </div>
      <FaceLabelingForm
        projectId={projectId}
        assetId={assetId}
        predictions={predictions}
      />
    </>
  )
}

FaceLabelingLabels.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
  predictions: PropTypes.arrayOf(PropTypes.shape({}).isRequired).isRequired,
}

export default FaceLabelingLabels
