import { colors, spacing, constants, typography } from '../Styles'

import Card from '../Card'

const BAR_HEIGHT = 16

const ProjectCards = () => {
  return (
    <Card title="Project Usage Plan">
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
          <img src="/icons/videos.png" alt="" width="32px" /> Video: 100 hours
        </h4>
        <div>
          <div
            css={{
              paddingTop: spacing.normal,
              paddingBottom: spacing.base,
              textAlign: 'right',
              color: colors.key.one,
            }}>
            Available:{' '}
            <span css={{ fontWeight: typography.weight.bold }}>100/hrs</span>
          </div>
          <div
            css={{
              height: BAR_HEIGHT,
              backgroundColor: colors.key.one,
              borderRadius: constants.borderRadius.small,
            }}
          />
        </div>
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
          <img src="/icons/images.png" alt="" width="32px" /> Image / Documents:
          100,000
        </h4>
        <div>
          <div
            css={{
              paddingTop: spacing.normal,
              paddingBottom: spacing.base,
              textAlign: 'right',
              color: colors.key.one,
            }}>
            Available:{' '}
            <span css={{ fontWeight: typography.weight.bold }}>100,000</span>
          </div>
          <div
            css={{
              height: BAR_HEIGHT,
              backgroundColor: colors.key.one,
              borderRadius: constants.borderRadius.small,
            }}
          />
        </div>
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
        <span>
          Contact your Account Manager to add additional modules and resources
          to your plan.
        </span>
      </div>
    </Card>
  )
}

export default ProjectCards
