import PropTypes from 'prop-types'
import { colors, spacing } from '../Styles'

const Button = ({ status }) => {
  const backgroundColor = {
    Active: colors.blue1,
    Paused: colors.yellow1,
    Canceled: colors.grey2,
    Finished: colors.green1,
  }

  return (
    <div
      className={`Button Button__${status}`}
      css={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: `${spacing.base}px`,
        height: `${spacing.comfy}px`,
        width: 'auto',
        borderRadius: `${spacing.base / 2}px`,
        color: status === 'Canceled' ? colors.black : colors.primaryFont,
        backgroundColor: backgroundColor[status],
      }}>
      {status}
    </div>
  )
}

Button.propTypes = {
  status: PropTypes.oneOf(['Active', 'Paused', 'Canceled', 'Finished'])
    .isRequired,
}

export default Button
