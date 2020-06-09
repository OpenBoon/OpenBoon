import PropTypes from 'prop-types'

import subscriptionShape from '../Subscription/shape'

import { constants, spacing, typography } from '../Styles'

const IMG_WIDTH = 32

const AccountUsagePlan = ({ subscriptions }) => {
  const {
    tier,
    usage: { videoHours: videoUsage, imageCount: imageUsage },
  } = subscriptions[0]

  return (
    <>
      <div
        css={{
          fontSize: typography.size.medium,
          lineHeight: typography.height.medium,
          fontWeight: typography.weight.medium,
          paddingBottom: spacing.moderate,
        }}
      >
        Project Tier: {tier.charAt(0).toUpperCase() + tier.substring(1)}
      </div>
      <h3
        css={{
          paddingBottom: spacing.comfy,
          fontSize: typography.size.regular,
          lineHeight: typography.height.regular,
        }}
      >
        Usage:
      </h3>
      <div
        css={{
          paddingBottom: spacing.normal,
          borderBottom: constants.borders.tabs,
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
          <img src="/icons/videos.png" alt="" width={IMG_WIDTH} /> Video Hours
          : {videoUsage.toLocaleString()}
        </h4>
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
          <img src="/icons/images.png" alt="" width={IMG_WIDTH} /> Images /
          Documents: {imageUsage.toLocaleString()}
        </h4>
      </div>
    </>
  )
}

AccountUsagePlan.propTypes = {
  subscriptions: PropTypes.arrayOf(subscriptionShape).isRequired,
}

export default AccountUsagePlan
