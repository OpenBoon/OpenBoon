import PropTypes from 'prop-types'

import subscriptionShape from '../Subscription/shape'

import { colors, spacing, typography } from '../Styles'

import FormAlert from '../FormAlert'

import OverviewUsage from './Usage'

const IMG_WIDTH = 32

const AccountUsagePlan = ({ subscriptions }) => {
  const {
    id: subscriptionId,
    limits: { videoHours: videoLimit, imageCount: imageLimit },
    usage: { videoHours: videoUsage, imageCount: imageUsage },
    modules,
  } = subscriptions[0]

  const videoOverTime = videoUsage - videoLimit
  const imageOverTime = imageUsage - imageLimit

  return (
    <>
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
      <h3
        css={{
          paddingBottom: spacing.comfy,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
        }}
      >
        Usage Plan:
      </h3>
      <div css={{ paddingBottom: spacing.normal }}>
        <h4
          css={{
            display: 'flex',
            alignItems: 'center',
            img: {
              marginRight: spacing.moderate,
            },
          }}
        >
          <img src="/icons/videos.png" alt="" width={IMG_WIDTH} /> Video:{' '}
          {videoLimit.toLocaleString()} hours
        </h4>
        <OverviewUsage limit={videoLimit} usage={videoUsage} legend="/hrs" />
      </div>
      <div
        css={{
          paddingTop: spacing.normal,
          paddingBottom: spacing.normal,
        }}
      >
        <h4
          css={{
            display: 'flex',
            alignItems: 'center',
            img: {
              marginRight: spacing.moderate,
            },
          }}
        >
          <img src="/icons/images.png" alt="" width={IMG_WIDTH} /> Image /
          Documents: {imageLimit.toLocaleString()}
        </h4>
        <OverviewUsage limit={imageLimit} usage={imageUsage} legend="" />
      </div>
      <div
        css={{
          paddingTop: spacing.normal,
        }}
      >
        <h4 css={{ paddingBottom: spacing.small }}>Additional Modules:</h4>

        {modules.length === 0 ? (
          <span css={{ color: colors.structure.zinc, fontStyle: 'italic' }}>
            None
          </span>
        ) : (
          modules.map((module) => (
            <span
              key={subscriptionId + module}
              css={{
                fontFamily: 'Roboto Mono',
                color: colors.structure.zinc,
                display: 'block',
                paddingBottom: spacing.mini,
              }}
            >
              {module}
            </span>
          ))
        )}
      </div>
    </>
  )
}

AccountUsagePlan.propTypes = {
  subscriptions: PropTypes.arrayOf(subscriptionShape).isRequired,
}

export default AccountUsagePlan
