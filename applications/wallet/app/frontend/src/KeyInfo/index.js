import PropTypes from 'prop-types'
import { colors, constants, spacing, typography } from '../Styles'
import ClockSvg from '../Icons/clock.svg'

export const STATUS_COLORS = {
  failed: colors.signal.warning.base,
  skipped: colors.structure.zinc,
  succeeded: colors.signal.grass.base,
  running: colors.signal.canary.base,
  pending: colors.signal.sky.base,
}

const InfoKey = ({ status, duration }) => {
  return (
    <div
      css={{
        display: 'flex',
        padding: spacing.moderate,
        backgroundColor: colors.structureShades.iron,
        borderRadius: constants.borderRadius.small,
      }}>
      <div css={{ display: 'flex' }}>
        <div css={{ display: 'flex' }}>
          <div css={{ paddingRight: spacing.base }}>
            <ClockSvg width={20} color={colors.structureShades.steel} />
          </div>
          <div css={{ paddingRight: spacing.comfy }}>
            <div css={{ color: colors.structureShades.pebble }}>
              <div>Duration:</div>
              <div
                css={{
                  color: colors.structureShades.white,
                  fontWeight: typography.weight.bold,
                }}>{`${duration.hours} hr / ${duration.minutes} m`}</div>
            </div>
          </div>
        </div>
        {['Failed', 'Skipped', 'Succeeded', 'Running', 'Pending'].map(
          statusName => {
            return (
              <div
                key={statusName}
                css={{ display: 'flex', paddingRight: spacing.comfy }}>
                <div
                  css={{
                    width: 2,
                    height: '100%',
                    backgroundColor: STATUS_COLORS[statusName.toLowerCase()],
                  }}
                />
                <div
                  css={{
                    color: colors.structureShades.pebble,
                    paddingLeft: spacing.base,
                  }}>
                  <div>{`${statusName}:`}</div>
                  <div
                    css={{
                      color: colors.structureShades.white,
                      fontWeight: typography.weight.bold,
                    }}>
                    {status[statusName.toLowerCase()]}
                  </div>
                </div>
              </div>
            )
          },
        )}
      </div>
    </div>
  )
}

InfoKey.propTypes = {
  status: PropTypes.shape({
    isGenerating: PropTypes.bool,
    isCanceled: PropTypes.bool,
    canceledBy: PropTypes.string,
    failed: PropTypes.number,
    skipped: PropTypes.number,
    succeeded: PropTypes.number,
    running: PropTypes.number,
    pending: PropTypes.number,
  }).isRequired,
  duration: PropTypes.shape({
    days: PropTypes.number,
    hours: PropTypes.number,
    minutes: PropTypes.number,
    seconds: PropTypes.number,
  }).isRequired,
}

export default InfoKey
