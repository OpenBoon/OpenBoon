import PropTypes from 'prop-types'
import useSWR from 'swr'
import Link from 'next/link'

import { colors, constants, spacing } from '../Styles'

import Button, { VARIANTS as BUTTON_VARIANTS } from '../Button'
import FlashMessage, { VARIANTS as FLASH_VARIANTS } from '../FlashMessage'

const FaceLabelingTrainApply = ({ projectId }) => {
  const {
    data: { unappliedChanges },
  } = useSWR(`/api/v1/projects/${projectId}/faces/unapplied_changes/`)

  const {
    data: { jobId },
  } = useSWR(`/api/v1/projects/${projectId}/faces/training_job/`)

  return (
    <div
      css={{
        padding: spacing.normal,
        borderBottom: constants.borders.divider,
      }}
    >
      {jobId && (
        <>
          <FlashMessage variant={FLASH_VARIANTS.PROCESSING}>
            Face training in progress.{' '}
            <Link
              href="/[projectId]/jobs/[jobId]"
              as={`/${projectId}/jobs/${jobId}`}
              passHref
            >
              <a
                css={{
                  color: colors.signal.sky.base,
                  ':hover': { textDecoration: 'none' },
                }}
              >
                Check status.
              </a>
            </Link>
          </FlashMessage>
          <div css={{ paddingBottom: spacing.normal }} />
        </>
      )}
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
        {jobId ? 'Override Current Training & Re-apply' : 'Train & Apply'}
      </Button>
    </div>
  )
}

FaceLabelingTrainApply.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default FaceLabelingTrainApply
