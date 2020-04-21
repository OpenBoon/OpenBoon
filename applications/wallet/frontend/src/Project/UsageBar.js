import PropTypes from 'prop-types'

import { colors, spacing, typography, constants } from '../Styles'

const BAR_HEIGHT = 16

const ProjectUsageBar = ({ limit, usage, legend }) => {
  const available = limit - usage
  return (
    <div>
      <div
        css={{
          display: 'flex',
          justifyContent: 'space-between',
          fontFamily: 'Roboto Condensed',
        }}
      >
        {usage > 0 && (
          <div
            css={{
              paddingTop: spacing.normal,
              paddingBottom: spacing.base,
              textAlign: 'right',
              color: colors.structure.zinc,
            }}
          >
            Used:{' '}
            <span css={{ fontWeight: typography.weight.bold }}>
              {usage.toLocaleString()}
              {legend}
            </span>
          </div>
        )}
        <div />
        {available > 0 && (
          <div
            css={{
              paddingTop: spacing.normal,
              paddingBottom: spacing.base,
              textAlign: 'right',
              color: colors.key.four,
            }}
          >
            Available:{' '}
            <span css={{ fontWeight: typography.weight.bold }}>
              {available.toLocaleString()}
              {legend}
            </span>
          </div>
        )}
      </div>
      <div css={{ display: 'flex' }}>
        <div
          css={{
            flex: usage,
            height: BAR_HEIGHT,
            backgroundColor: colors.structure.steel,
            borderTopLeftRadius: constants.borderRadius.small,
            borderBottomLeftRadius: constants.borderRadius.small,
          }}
        />
        <div
          css={{
            flex: available,
            height: BAR_HEIGHT,
            backgroundColor: colors.key.four,
            borderTopRightRadius: constants.borderRadius.small,
            borderBottomRightRadius: constants.borderRadius.small,
          }}
        />
      </div>
    </div>
  )
}

ProjectUsageBar.propTypes = {
  limit: PropTypes.number.isRequired,
  usage: PropTypes.number.isRequired,
  legend: PropTypes.string.isRequired,
}

export default ProjectUsageBar
