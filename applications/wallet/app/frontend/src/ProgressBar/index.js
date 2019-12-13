import PropTypes from 'prop-types'
import { colors, constants } from '../Styles'

const CONTAINER_HEIGHT = 16
const CONTAINER_WIDTH = 200
const STATUS_COLORS = {
  succeeded: colors.green1,
  failed: colors.error,
  running: colors.blue1,
  pending: colors.grey6,
}

const ProgressBar = ({ status }) => {
  const { isCanceled, canceledBy } = status

  if (isCanceled) {
    return (
      <div css={{ color: colors.grey5 }}>{`Canceled by: ${canceledBy}`}</div>
    )
  }

  return (
    <div
      css={{
        display: 'flex',
        height: CONTAINER_HEIGHT,
        width: CONTAINER_WIDTH,
      }}>
      {['succeeded', 'failed', 'running', 'pending']
        .filter(statusName => {
          return status[statusName] > 0
        })
        .map(statusName => {
          return (
            <div
              key={statusName}
              css={{
                height: '100%',
                flex: `${status[statusName]} 0 auto`,
                backgroundColor: STATUS_COLORS[statusName],
                '&:first-of-type': {
                  borderTopLeftRadius: constants.borderRadius.small,
                  borderBottomLeftRadius: constants.borderRadius.small,
                },
                '&:last-of-type': {
                  borderTopRightRadius: constants.borderRadius.small,
                  borderBottomRightRadius: constants.borderRadius.small,
                },
              }}
            />
          )
        })}
    </div>
  )
}

ProgressBar.propTypes = {
  status: PropTypes.shape({
    isCanceled: PropTypes.bool,
    canceledBy: PropTypes.string,
    succeeded: PropTypes.number,
    failed: PropTypes.number,
    running: PropTypes.number,
    pending: PropTypes.number,
  }).isRequired,
}

export default ProgressBar
