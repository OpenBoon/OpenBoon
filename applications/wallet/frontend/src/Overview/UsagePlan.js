import useSWR from 'swr'
import PropTypes from 'prop-types'

import { colors, spacing, constants, typography } from '../Styles'

import ProjectUsageBar from '../Project/UsageBar'
import FormAlert from '../FormAlert'

const IMG_WIDTH = 32

const OverviewUsagePlan = ({ projectId }) => {
  const {
    data: { results: subscriptions },
  } = useSWR(`/api/v1/projects/${projectId}/subscriptions/`)

  if (subscriptions.length === 0) return null

  const {
    limits: { videoHours: videoLimit, imageCount: imageLimit },
    usage: { videoHours: videoUsage, imageCount: imageUsage },
  } = subscriptions[0]

  const videoOverTime = videoUsage - videoLimit
  const imageOverTime = imageUsage - imageLimit

  return (
    <div css={{ padding: spacing.spacious, borderTop: constants.borders.tabs }}>
      {(videoOverTime > 1 || imageOverTime > 1) && (
        <div css={{ marginTop: -spacing.normal, marginBottom: spacing.normal }}>
          <FormAlert setErrorMessage={false}>
            <div css={{ fontWeight: typography.weight.regular }}>
              You are{' '}
              {videoOverTime > 1 && (
                <>
                  <strong>{videoOverTime} hours over</strong> your video plan
                </>
              )}
              {videoOverTime > 1 && imageOverTime > 1 && ' and '}
              {imageOverTime > 1 && (
                <>
                  <strong>{imageOverTime} assets over</strong> your image &amp;
                  documents plan
                </>
              )}
              . Contact your Account Manager to add more resources.
            </div>
          </FormAlert>
        </div>
      )}
      <div
        css={{
          borderBottom: constants.borders.tabs,
          paddingBottom: spacing.normal,
        }}>
        <h4
          css={{
            display: 'flex',
            alignItems: 'center',
            img: {
              marginRight: spacing.moderate,
            },
          }}>
          <img src="/icons/videos.png" alt="" width={IMG_WIDTH} /> Video:{' '}
          {videoLimit.toLocaleString()} hours
        </h4>
        <ProjectUsageBar limit={videoLimit} usage={videoUsage} legend="/hrs" />
      </div>
      <div
        css={{
          borderBottom: constants.borders.tabs,
          paddingTop: spacing.normal,
          paddingBottom: spacing.normal,
        }}>
        <h4
          css={{
            display: 'flex',
            alignItems: 'center',
            img: {
              marginRight: spacing.moderate,
            },
          }}>
          <img src="/icons/images.png" alt="" width={IMG_WIDTH} /> Image /
          Documents: {imageLimit.toLocaleString()}
        </h4>
        <ProjectUsageBar limit={imageLimit} usage={imageUsage} legend="" />
      </div>
      <div
        css={{
          borderBottom: constants.borders.tabs,
          paddingTop: spacing.normal,
          paddingBottom: spacing.normal,
        }}>
        <h4>Additional Modules:</h4>
        <span css={{ color: colors.structure.zinc, fontStyle: 'italic' }}>
          None
        </span>
      </div>
      <div css={{ paddingTop: spacing.normal, color: colors.structure.zinc }}>
        Contact your Account Manager to add additional modules and resources to
        your plan.
      </div>
    </div>
  )
}

OverviewUsagePlan.propTypes = {
  projectId: PropTypes.string.isRequired,
}

export default OverviewUsagePlan
