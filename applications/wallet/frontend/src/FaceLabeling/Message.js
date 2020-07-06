import { useEffect } from 'react'
import PropTypes from 'prop-types'
import Link from 'next/link'
import { cache } from 'swr'

import { colors, spacing } from '../Styles'

import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'

const FaceLabelingMessage = ({
  projectId,
  previousJobId,
  jobId,
  error,
  setPreviousJobId,
}) => {
  useEffect(() => {
    // jobId goes from "" to "123"
    // Training has started, update previousJobId
    if (!previousJobId && jobId) {
      setPreviousJobId(jobId)
    }
    // jobId goes from "123" to ""
    // Training is complete, reset the cache
    if (previousJobId && !jobId) {
      cache
        .keys()
        .filter(
          (key) => key.includes('/faces/') && !key.includes('/faces/status/'),
        )
        .forEach((key) => cache.delete(key))
    }
  }, [previousJobId, jobId, setPreviousJobId])

  if (error) {
    return (
      <div css={{ paddingBottom: spacing.normal }}>
        <FlashMessage variant={FLASH_VARIANTS.ERROR}>{error}</FlashMessage>
      </div>
    )
  }

  if (jobId) {
    return (
      <div css={{ paddingBottom: spacing.normal }}>
        <FlashMessage variant={FLASH_VARIANTS.PROCESSING}>
          Face training in progress.{' '}
          <Link
            href="/[projectId]/jobs/[jobId]"
            as={`/${projectId}/jobs/${jobId}`}
            passHref
          >
            <a css={{ color: colors.signal.sky.base }}>Check Status</a>
          </Link>
        </FlashMessage>
      </div>
    )
  }

  if (previousJobId && !jobId) {
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
  jobId: PropTypes.string.isRequired,
  error: PropTypes.string.isRequired,
  setPreviousJobId: PropTypes.func.isRequired,
}

export default FaceLabelingMessage
