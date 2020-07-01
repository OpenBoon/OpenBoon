import PropTypes from 'prop-types'
import Link from 'next/link'

import { colors, spacing } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'

const FaceLabelingMessage = ({
  projectId,
  previousJobId,
  currentJobId,
  error,
}) => {
  if (error) {
    return (
      <div css={{ paddingBottom: spacing.normal }}>
        <FlashMessage variant={FLASH_VARIANTS.ERROR}>{error}</FlashMessage>
      </div>
    )
  }

  if (currentJobId) {
    return (
      <div css={{ paddingBottom: spacing.normal }}>
        <FlashMessage variant={FLASH_VARIANTS.PROCESSING}>
          Face training in progress.{' '}
          <Link
            href="/[projectId]/jobs/[jobId]"
            as={`/${projectId}/jobs/${currentJobId}`}
            passHref
          >
            <a css={{ color: colors.signal.sky.base }}>Check Status</a>
          </Link>
        </FlashMessage>
      </div>
    )
  }

  if (!!previousJobId && !currentJobId) {
    return (
      <div css={{ paddingBottom: spacing.normal }}>
        <FlashMessage
          css={{ paddingBottom: spacing.normal }}
          variant={FLASH_VARIANTS.SUCCESS}
        >
          Face training complete.
        </FlashMessage>
      </div>
    )
  }

  return null
}

FaceLabelingMessage.propTypes = {
  projectId: PropTypes.string.isRequired,
  previousJobId: PropTypes.string.isRequired,
  currentJobId: PropTypes.string.isRequired,
  error: PropTypes.string.isRequired,
}

export default FaceLabelingMessage
