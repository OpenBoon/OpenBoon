import PropTypes from 'prop-types'
import useSWR from 'swr'

import { colors, constants, spacing } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'

const FaceLabelingContent = ({ projectId, assetId }) => {
  const { data: asset } = useSWR(
    `/api/v1/projects/${projectId}/assets/${assetId}/`,
  )

  const {
    metadata: {
      source: { filename },
    },
  } = asset

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
          isDisabled
        >
          Train &amp; Apply
        </Button>
      </div>
      <div
        css={{
          padding: spacing.normal,
          borderBottom: constants.borders.divider,
          color: colors.signal.sky.base,
        }}
      >
        {filename}
      </div>
    </>
  )
}

FaceLabelingContent.propTypes = {
  projectId: PropTypes.string.isRequired,
  assetId: PropTypes.string.isRequired,
}

export default FaceLabelingContent
