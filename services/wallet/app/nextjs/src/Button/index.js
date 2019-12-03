import PropTypes from 'prop-types'
import { colors, spacing, constants } from '../Styles'

const BACKGROUND_COLORS = {
  Active: colors.blue1,
  Paused: colors.yellow1,
  Canceled: colors.grey2,
  Finished: colors.green1,
}

const BUTTON_HEIGHT = 24

const Button = ({ status }) => {
  return (
    <div
      css={{
        display: 'inline-flex',
        alignItems: 'center',
        padding: spacing.base,
        height: BUTTON_HEIGHT,
        width: 'auto',
        borderRadius: constants.borderRadius.small,
        color: status === 'Canceled' ? colors.black : colors.primaryFont,
        backgroundColor: BACKGROUND_COLORS[status],
      }}>
      {status}
    </div>
  )
}

Button.propTypes = {
  status: PropTypes.oneOf(Object.keys(BACKGROUND_COLORS)).isRequired,
}

export default Button
